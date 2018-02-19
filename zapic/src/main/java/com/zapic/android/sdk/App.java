package com.zapic.android.sdk;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.view.ViewManager;
import android.view.ViewParent;
import android.webkit.JavascriptInterface;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

final class App implements ConnectivityListener {
    /**
     * A communication bridge that dispatches messages from the Zapic JavaScript application.
     *
     * @author Kyle Dodson
     * @since 1.0.0
     */
    final class AppJavascriptInterface {
        /**
         * Dispatches a message from the Zapic JavaScript application.
         * <p>
         * This is <i>not</i> invoked on the UI thread.
         *
         * @param message The message.
         */
        @JavascriptInterface
        @WorkerThread
        public void dispatch(@Nullable final String message) {
            JSONObject action;
            try {
                action = new JSONObject(message);
            } catch (JSONException e) {
                Log.e(App.TAG, "Failed to parse serialized action", e);
                return;
            }

            String type;
            try {
                type = action.getString("type");
            } catch (JSONException e) {
                Log.e(App.TAG, "The action does not have a type", e);
                return;
            }

            switch (type) {
                case "APP_LOADED": {
                    App.this.mLoaded = true;
                    break;
                }
                case "APP_STARTED": {
                    App.this.mStarted = true;

                    ZapicActivity activity = App.this.mActivity;
                    if (activity != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                            App.this.open();
                            }
                        });
                    }

                    break;
                }
                case "CLOSE_PAGE_REQUESTED": {
                    ZapicActivity activity = App.this.mActivity;
                    if (activity != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                            ZapicActivity activity = App.this.mActivity;
                            if (activity != null) {
                                activity.finish();
                            }
                            }
                        });
                    }

                    break;
                }
                case "LOGGED_IN": {
                    try {
                        String payload = action.getString("payload");
                        Zapic.setPlayerId(payload);
                    } catch (JSONException ignored) {
                        Zapic.setPlayerId(null);
                    }
                }
                case "LOGIN": {
                    Log.d(App.TAG, message);
                    break;
                }
                case "PAGE_READY": {
                    ZapicActivity activity = App.this.mActivity;
                    if (activity != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ZapicActivity activity = App.this.mActivity;
                                if (activity != null) {
                                    activity.openAppPage();
                                }
                            }
                        });
                    }
                }
                case "SHOW_BANNER": {
                    Log.d(App.TAG, message);
                    break;
                }
                default: {
                    Log.e(App.TAG, String.format("The action type is invalid: %s", type));
                    break;
                }
            }
        }
    }

    /**
     * A utility that overrides the default {@link WebView} behaviors.
     *
     * @author Kyle Dodson
     * @since 1.0.0
     */
    final class AppWebViewClient extends WebViewClient {
// TODO: Handle errors loading the initial HTTP content.
//    @MainThread
//    @Override
//    @RequiresApi(Build.VERSION_CODES.M)
//    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
//        // TODO: Transition to an offline page.
//    }

// TODO: Handle errors loading the initial HTTP content.
//    @MainThread
//    @Override
//    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
//        // TODO: Transition to an offline page.
//    }

        @MainThread
        @Override
        @RequiresApi(Build.VERSION_CODES.O)
        public boolean onRenderProcessGone(@NonNull final WebView view, @NonNull final RenderProcessGoneDetail detail) {
            final boolean crashed = detail.didCrash();
            if (crashed) {
                Log.e(App.TAG, "The WebView has crashed");
            } else {
                Log.e(App.TAG, "The WebView has been stopped to reclaim memory");
            }

            if (App.this.mActivity != null) {
                App.this.mActivity.openLoadingPage();
                App.this.mWebView = null;
            }

            return true;
        }

        @MainThread
        @Override
        @RequiresApi(Build.VERSION_CODES.N)
        public boolean shouldOverrideUrlLoading(@Nullable final WebView view, @Nullable final WebResourceRequest request) {
            if (view == null || request == null) {
                return false;
            }

            Uri url = request.getUrl();
            return url != null && this.overrideUrlLoading(view, request.getUrl());
        }

        @MainThread
        @Override
        public boolean shouldOverrideUrlLoading(@Nullable final WebView view, @Nullable final String url) {
            return !(view == null || url == null) && this.overrideUrlLoading(view, Uri.parse(url));
        }

        /**
         * Intercepts {@link WebView} navigation events and, optionally, overrides the navigation
         * behavior.
         * <p>
         * This currently overrides navigation events to {@code market://*} URLs by opening the Google
         * Play Store app.
         *
         * @param view The {@link WebView}.
         * @param url  The target {@link Uri} of the navigation event.
         * @return     {@code true} if the navigation behavior was overridden; {@code false} if the
         *             navigation behavior was not overridden.
         */
        @MainThread
        private boolean overrideUrlLoading(@NonNull final WebView view, @NonNull final Uri url) {
            final String scheme = url.getScheme();
            if (scheme != null && scheme.equalsIgnoreCase("market")) {
                try {
                    // Create a Google Play Store app intent.
                    final Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(url);

                    // Open the Google Play Store app.
                    final Context context = view.getContext();
                    context.startActivity(intent);
                } catch (ActivityNotFoundException ignored) {
                }

                // Prevent the WebView from navigating to an invalid URL.
                return true;
            }

            // TODO: Prevent navigation to external URLs.
            return false;
        }
    }

    /**
     * The Zapic JavaScript application URL.
     */
    @NonNull
    private static final String APP_URL = "https://app.zapic.net";

    /**
     * The tag used to identify log messages.
     */
    @NonNull
    private static final String TAG = "App";

    /**
     * The variable name of the {@link AppJavascriptInterface} instance.
     */
    @NonNull
    private static final String VARIABLE_NAME = "androidWebView";

    /**
     * The {@link ZapicActivity}.
     * <p>
     * This should be {@code null} when the game's activity is visible. This should be
     * non-{@code null} when the {@link ZapicActivity} is visible.
     */
    @Nullable
    private ZapicActivity mActivity;

    /**
     * The currently executing asynchronous task.
     */
    @Nullable
    private AsyncTask mAsyncTask;

    /**
     * The Android application's cache directory.
     */
    @Nullable
    private File mCacheDir;

    /**
     * A value that indicates whether the Android device has network connectivity.
     */
    private boolean mConnected;

    /**
     * A value that indicates whether the Zapic JavaScript application has been loaded.
     */
    private boolean mLoaded;

    /**
     * A value that indicates whether the Zapic JavaScript application has been started.
     */
    private boolean mStarted;

    /**
     * The {@link WebView}.
     */
    @Nullable
    private WebView mWebView;

    /**
     * Creates a new {@link App} instance
     */
    App() {
        this.mActivity = null;
        this.mAsyncTask = null;
        this.mCacheDir = null;
        this.mConnected = false;
        this.mLoaded = false;
        this.mStarted = false;
        this.mWebView = null;
    }

    /**
     * Gets the {@link WebView}.
     *
     * @return The {@link WebView}.
     */
    @Nullable
    WebView getWebView() {
        return this.mWebView;
    }

    @MainThread
    void loadWebView(@NonNull final AppSource appSource) {
        this.mAsyncTask = null;

        if (this.mWebView != null) {
            this.mWebView.loadDataWithBaseURL(App.APP_URL, appSource.getHtml(), "text/html", "utf-8", App.APP_URL);
        }
    }

    @Override
    public void onConnected() {
        if (this.mConnected) {
            return;
        }

        this.mConnected = true;

        // TODO: Start async task if needed.
    }

    @Override
    public void onDisconnected() {
        if (!this.mConnected) {
            return;
        }

        this.mConnected = false;
    }

    @MainThread
    private void open() {
        if (this.mActivity != null && this.mWebView != null) {
            String page = this.mActivity.getIntent().getStringExtra("page");
            if (page == null) {
                page = "default";
            }

            final String escapedPage = page.replace("'", "\\'");
            this.mWebView.evaluateJavascript("window.zapic.dispatch({ type: 'OPEN_PAGE', payload: '" + escapedPage + "' })", null);
        }
    }

    @MainThread
    void setActivity(@Nullable final ZapicActivity activity) {
        if (activity == null && this.mActivity != null) {
            WebView webView = App.this.mWebView;
            if (webView != null) {
                webView.evaluateJavascript("window.zapic.dispatch({ type: 'CLOSE_PAGE' })", null);
            }
        }

        this.mActivity = activity;

        if (this.mStarted) {
            this.open();
        }
    }

    @MainThread
    @SuppressLint("SetJavaScriptEnabled")
    void start(@NonNull final Context context) {
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // This enables WebView debugging for all WebViews in the current process. Changes to
            // this value are accepted only before the WebView process is created.
            WebView.setWebContentsDebuggingEnabled(true);
        }

        // We use the application context to ensure we don't leak an activity.
        this.mWebView = new WebView(context.getApplicationContext());
        this.mWebView.addJavascriptInterface(this.new AppJavascriptInterface(), App.VARIABLE_NAME);
        this.mWebView.getSettings().setAllowContentAccess(false);
        this.mWebView.getSettings().setAllowFileAccess(false);
        this.mWebView.getSettings().setAllowFileAccessFromFileURLs(false);
        this.mWebView.getSettings().setAllowUniversalAccessFromFileURLs(false);
        this.mWebView.getSettings().setDomStorageEnabled(true);
        this.mWebView.getSettings().setGeolocationEnabled(false);
        this.mWebView.getSettings().setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }
        this.mWebView.getSettings().setSaveFormData(false);
        this.mWebView.getSettings().setSupportZoom(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.mWebView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_WAIVED, false);
        }
        this.mWebView.setWebViewClient(this.new AppWebViewClient());

        // Get the Android application's cache directory.
        this.mCacheDir = context.getCacheDir();

        // Start an asynchronous task to get the web client application.
        AppSourceAsyncTask asyncTask = new AppSourceAsyncTask(this, App.APP_URL, this.mCacheDir);
        this.mAsyncTask = asyncTask;
        asyncTask.execute();
    }

    @MainThread
    void stop() {
        if (this.mAsyncTask != null) {
            this.mAsyncTask.cancel(true);
            this.mAsyncTask = null;
        }

        if (this.mActivity != null) {
            this.mActivity.close();
            this.mActivity = null;
        }

        if (this.mWebView != null) {
            final ViewParent parent = this.mWebView.getParent();
            if (parent instanceof ViewManager) {
                ((ViewManager)parent).removeView(this.mWebView);
            }

            this.mWebView.destroy();
            this.mWebView = null;
        }
    }

    @MainThread
    void submitEvent(@NonNull final JSONObject event) {}
}
