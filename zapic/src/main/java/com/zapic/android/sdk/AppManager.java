package com.zapic.android.sdk;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.AnyThread;
import android.support.annotation.CheckResult;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

final class AppManager {
    private enum State {
        UNLOADED,
        LOADED
    }

    @NonNull
    private static final String JAVASCRIPT_VARIABLE_NAME = "androidWebView";

    @NonNull
    private static final String TAG = "AppManager";

    /**
     * The web client URL.
     */
    @NonNull
    private static final String WEB_CLIENT_URL = "https://app.zapic.net";

    private static final Object instanceLock = new Object();

    private static WeakReference<AppManager> instance = new WeakReference<>(null);

    @NonNull
    static AppManager getInstance() {
        synchronized (AppManager.instanceLock) {
            AppManager appManager = AppManager.instance.get();
            if (appManager == null) {
                Log.i(TAG, "Creating a new instance of AppManager");
                appManager = new AppManager();
                AppManager.instance = new WeakReference<>(appManager);
            }

            return appManager;
        }
    }

    @Nullable
    private AppJavaBridge mAppJavaBridge;

    @Nullable
    private AppJavaScriptBridge mAppJavaScriptBridge;

    @Nullable
    private AsyncTask mAsyncTask;

    @Nullable
    private File mCacheDir;

    @NonNull
    private final ArrayList<StateChangedListener> mListeners;

    private volatile boolean mOffline;

    private volatile State mState;

    private int mViewCount;

    @Nullable
    private WebView mWebView;

    private AppManager() {
        this.mAppJavaBridge = null;
        this.mAppJavaScriptBridge = null;
        this.mAsyncTask = null;
        this.mCacheDir = null;
        this.mListeners = new ArrayList<>();
        this.mOffline = false;
        this.mState = State.UNLOADED;
        this.mViewCount = 0;
        this.mWebView = null;
    }

    @MainThread
    @Nullable
    WebView getWebView() {
        return this.mWebView;
    }

    @AnyThread
    @CheckResult
    boolean isAppStarted() {
        return this.mState == State.LOADED;
    }

    @AnyThread
    @CheckResult
    boolean isOffline() { return this.mOffline; }

    /**
     * Creates the {@link WebView} if it has not already been created and asynchronously loads the
     * web client application.
     *
     * @param context A context belonging to the Android application.
     */
    @MainThread
    void onViewCreated(@NonNull final Context context) {
        ++this.mViewCount;
        if (this.mViewCount == 1) {
            // Create the WebView.
            assert this.mWebView == null : "mWebView != null";
            this.mWebView = this.createWebView(context);

            // Get the Android application's cache directory.
            this.mCacheDir = context.getCacheDir();

            if (!this.mOffline) {
                // Start an asynchronous task to get the web client application.
                AppSourceAsyncTask asyncTask = new AppSourceAsyncTask(this, AppManager.WEB_CLIENT_URL, this.mCacheDir);
                this.mAsyncTask = asyncTask;
                asyncTask.execute();
            }
        }
    }

    @MainThread
    void onViewDestroyed() {
        --this.mViewCount;
        if (this.mViewCount == 0) {
            // Cancel any asynchronous task.
            if (this.mAsyncTask != null) {
                this.mAsyncTask.cancel(true);
                this.mAsyncTask = null;
            }

            // Destroy the WebView.
            if (this.mWebView != null) {
                this.mWebView.destroy();
                this.mWebView = null;
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private WebView createWebView(@NonNull final Context context) {
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // This enables WebView debugging for all WebViews in the current process. Changes
            // to this value are accepted only before the WebView process is created.
            WebView.setWebContentsDebuggingEnabled(true);
        }

        // The WebView is tied to the application's context to ensure we don't create an
        // activity memory leak.
        Context applicationContext = context.getApplicationContext();
        WebView webView = new WebView(applicationContext);

        if (this.mAppJavaBridge == null) {
            this.mAppJavaBridge = new AppJavaBridge(webView);
        }

        if (this.mAppJavaScriptBridge == null) {
            this.mAppJavaScriptBridge = new AppJavaScriptBridge();
        }

        webView.addJavascriptInterface(this.mAppJavaScriptBridge, AppManager.JAVASCRIPT_VARIABLE_NAME);
        webView.getSettings().setAllowContentAccess(false);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowFileAccessFromFileURLs(false);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(false);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setGeolocationEnabled(false);
        webView.getSettings().setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }
        webView.getSettings().setSaveFormData(false);
        webView.getSettings().setSupportZoom(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, true);
        }
        webView.setWebViewClient(new WebViewClient() {
            @Override
            @RequiresApi(Build.VERSION_CODES.O)
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                boolean crashed = detail.didCrash();
                if (crashed) {
                    Log.e(TAG, "The WebView has crashed");
                } else {
                    Log.e(TAG, "The WebView has been stopped to reclaim memory");
                }

                return true;
            }

            @Override
            @RequiresApi(Build.VERSION_CODES.N)
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (view == null || request == null) {
                    return false;
                }

                Uri url = request.getUrl();
                return url != null && AppManager.overrideUrlLoading(view, request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return !(view == null || url == null) && AppManager.overrideUrlLoading(view, Uri.parse(url));
            }
        });

        return webView;
    }

    @MainThread
    void loadWebView(@NonNull final AppSource appSource) {
        this.mAsyncTask = null;

        if (this.mWebView != null) {
            this.mWebView.loadDataWithBaseURL(AppManager.WEB_CLIENT_URL, appSource.getHtml(), "text/html", "utf-8", AppManager.WEB_CLIENT_URL);

            // Notify listeners.
            this.mState = State.LOADED;
            for (StateChangedListener listener : this.mListeners) {
                listener.onAppStarted();
            }
        }
    }

    /**
     * Intercepts {@see android.webkit.WebView} navigation events and, optionally, overrides the
     * navigation behavior.
     * <p>
     * This currently only overrides navigation events to {@code market://*} URLs by opening the
     * the Google Play Store app.
     *
     * @param view The {@see android.webkit.WebView}.
     * @param url  The {@see Uri} of the navigation event.
     * @return     {@code true} if the navigation behavior was overridden; {@code false} if the
     *             navigation behavior was not overridden.
     */
    private static boolean overrideUrlLoading(@NonNull final WebView view, @NonNull final Uri url) {
        final String scheme = url.getScheme();
        if (scheme != null && scheme.equalsIgnoreCase("market")) {
            try {
                // Create a Google Play Store app intent.
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(url);

                // Open the Google Play Store app.
                Context context = view.getContext();
                context.startActivity(intent);
            } catch (ActivityNotFoundException ignored) {
            }

            // Prevent the WebView from navigating to an invalid URL.
            return true;
        }

        return false;
    }

    @MainThread
    void setOffline() {
        if (this.mOffline) {
            return;
        }

        // Cancel any asynchronous task.
        if (this.mAsyncTask != null) {
            this.mAsyncTask.cancel(true);
            this.mAsyncTask = null;
        }

        // Notify listeners.
        this.mOffline = true;
        for (StateChangedListener listener : this.mListeners) {
            listener.onOffline();
        }
    }

    @MainThread
    void setOnline() {
        if (!this.mOffline) {
            return;
        }

        // Notify listeners.
        this.mOffline = false;
        for (StateChangedListener listener : this.mListeners) {
            listener.onOnline();
        }

        // Start an asynchronous task to get the web client application.
        if (this.mState == State.UNLOADED && this.mCacheDir != null) {
            AppSourceAsyncTask asyncTask = new AppSourceAsyncTask(this, AppManager.WEB_CLIENT_URL, this.mCacheDir);
            this.mAsyncTask = asyncTask;
            asyncTask.execute();
        }
    }

    interface StateChangedListener {
        @MainThread
        void onAppStarted();

        @MainThread
        void onOffline();

        @MainThread
        void onOnline();
    }

    @MainThread
    void addStateChangedListener(@NonNull final StateChangedListener listener) {
        if (!this.mListeners.contains(listener)) {
            this.mListeners.add(listener);
        }
    }

    @MainThread
    void removeStateChangedListener(@NonNull final StateChangedListener listener) {
        this.mListeners.remove(listener);
    }
}
