package com.zapic.sdk.android;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.MutableContextWrapper;
import android.net.Uri;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.support.annotation.AnyThread;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;

final class WebViewManager {
    /**
     * The tag used to identify log messages.
     */
    @NonNull
    private static final String TAG = "WebViewManager";

    /**
     * A weak reference to the {@link WebViewManager} instance.
     * <p>
     * A strong reference to the {@link WebViewManager} instance is kept by {@link ZapicActivity}
     * and {@link ZapicFragment}.
     */
    @NonNull
    private static WeakReference<WebViewManager> INSTANCE = new WeakReference<>(null);

    /**
     * The JavaScript variable name of the {@link WebViewJavascriptInterface} instance.
     */
    @NonNull
    private static final String VARIABLE_NAME = "androidWebView";

    @AnyThread
    static WebViewManager getInstance() {
        WebViewManager instance = WebViewManager.INSTANCE.get();
        if (instance == null) {
            synchronized (WebViewManager.class) {
                instance = WebViewManager.INSTANCE.get();
                if (instance == null) {
                    instance = new WebViewManager();
                    WebViewManager.INSTANCE = new WeakReference<>(instance);
                }
            }
        }

        return instance;
    }

    /**
     * The {@link ZapicActivity} instance.
     */
    @Nullable
    private ZapicActivity mActivity;

    @Nullable
    private AsyncTask mAsyncTask;

    /**
     * The list of {@link ZapicFragment} instances.
     */
    @NonNull
    private final ArrayList<ZapicFragment> mFragments;

    /**
     * The handler used to dispatch messages to the main thread.
     */
    @Nullable
    private Handler mHandler;

    @Nullable
    private ValueCallback<Uri[]> mImageUploadCallback;

    private boolean mLoaded;

    @NonNull
    private final LinkedList<JSONObject> mPendingEvents;

    @NonNull
    private final Object mPendingEventsLock;

    private boolean mStarted;

    @Nullable
    private WebView mWebView;

    @AnyThread
    private WebViewManager() {
        this.mActivity = null;
        this.mAsyncTask = null;
        this.mFragments = new ArrayList<>();
        this.mHandler = null;
        this.mImageUploadCallback = null;
        this.mLoaded = false;
        this.mPendingEvents = new LinkedList<>();
        this.mPendingEventsLock = new Object();
        this.mStarted = false;
        this.mWebView = null;
    }

    @MainThread
    void cancelImageUpload() {
        if (this.mImageUploadCallback != null) {
            this.mImageUploadCallback.onReceiveValue(null);
            this.mImageUploadCallback = null;
        }
    }

    @MainThread
    void cancelLoadApp() {
        this.showOfflineFragment();
    }

    @MainThread
    @SuppressLint("SetJavaScriptEnabled")
    private void createWebView(@NonNull final Context context) {
        if (BuildConfig.DEBUG) {
            // This enables WebView debugging for all WebViews in the current process. Changes to
            // this value are accepted only before the WebView process is created.
            WebView.setWebContentsDebuggingEnabled(true);
        }

        this.mWebView = new WebView(new MutableContextWrapper(context.getApplicationContext()));
        this.mWebView.addJavascriptInterface(new WebViewJavascriptInterface(context), VARIABLE_NAME);
        this.mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.mWebView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, true);
        }

        WebSettings webSettings = this.mWebView.getSettings();
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }

// These were from the old project:
//        webSettings.setAllowContentAccess(false);
//        webSettings.setAllowFileAccess(false);
//        webSettings.setAllowFileAccessFromFileURLs(false);
//        webSettings.setAllowUniversalAccessFromFileURLs(false);
//        webSettings.setGeolocationEnabled(false);
//        webSettings.setSaveFormData(false);
//        webSettings.setSupportZoom(false);

// These are in the Android-SmartWebView:
//        webSettings.setSaveFormData(ASWP_SFORM);
//        webSettings.setSupportZoom(ASWP_ZOOM);
//        webSettings.setGeolocationEnabled(ASWP_LOCATION);
//        webSettings.setAllowFileAccess(true);
//        webSettings.setAllowFileAccessFromFileURLs(true);
//        webSettings.setAllowUniversalAccessFromFileURLs(true);
//        webSettings.setUseWideViewPort(true);
//        webSettings.setDomStorageEnabled(true);
//        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);

        this.mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            public boolean onShowFileChooser(@NonNull final WebView webView, @NonNull final ValueCallback<Uri[]> filePathCallback, @NonNull final FileChooserParams fileChooserParams) {
                if (WebViewManager.this.mActivity == null) {
                    return false;
                }

                String[] acceptTypes = fileChooserParams.getAcceptTypes();
                if (acceptTypes == null || acceptTypes.length != 1 || !acceptTypes[0].equalsIgnoreCase("image/*") || fileChooserParams.getMode() != FileChooserParams.MODE_OPEN) {
                    return false;
                }

                WebViewManager.this.mImageUploadCallback = filePathCallback;
                WebViewManager.this.mActivity.showImagePrompt();
                return true;
            }
        });

        this.mWebView.setWebViewClient(new WebViewClient() {
            @Override
            @RequiresApi(Build.VERSION_CODES.M)
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                // TODO: Show an error to the user.
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // TODO: Show an error to the user.
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // TODO: Show an error to the user.
            }

            @Override
            @RequiresApi(Build.VERSION_CODES.O)
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                if (WebViewManager.this.mWebView != view) {
                    return false;
                }

                if (BuildConfig.DEBUG) {
                    final boolean crashed = detail.didCrash();
                    if (crashed) {
                        Log.e(TAG, "The WebView has crashed");
                    } else {
                        Log.e(TAG, "The WebView has been stopped to reclaim memory");
                    }
                }

                if (WebViewManager.this.mActivity != null) {
                    WebViewManager.this.finishActivity();
                }

                WebViewManager.this.mImageUploadCallback = null;

                if (WebViewManager.this.mAsyncTask != null) {
                    WebViewManager.this.mAsyncTask.cancel(true);
                    WebViewManager.this.mAsyncTask = null;
                }

                if (WebViewManager.this.mWebView != null) {
                    WebViewManager.this.mWebView.destroy();
                    WebViewManager.this.mWebView = null;
                    WebViewManager.this.mLoaded = false;
                    WebViewManager.this.mStarted = false;
                }

                return true;
            }

            @Override
            @RequiresApi(Build.VERSION_CODES.N)
            public boolean shouldOverrideUrlLoading(@Nullable final WebView view, @Nullable final WebResourceRequest request) {
                if (view == null || request == null) {
                    return false;
                }

                Uri url = request.getUrl();
                return url != null && WebViewManager.this.overrideUrl(view, request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(@Nullable final WebView view, @Nullable final String url) {
                return !(view == null || url == null) && WebViewManager.this.overrideUrl(view, Uri.parse(url));
            }
        });
    }

    @MainThread
    private void createEventHandler(@NonNull final Context context) {
        if (this.mHandler == null) {
            this.mHandler = new Handler(context.getApplicationContext().getMainLooper(), new Callback() {
                @Override
                public boolean handleMessage(@Nullable final Message msg) {
                    if (msg == null || msg.what != 0) {
                        return false;
                    }

                    final JSONObject event = (JSONObject) msg.obj;
                    if (WebViewManager.this.mWebView == null) {
                        synchronized (WebViewManager.this.mPendingEventsLock) {
                            if (WebViewManager.this.mPendingEvents.size() == 1000) {
                                WebViewManager.this.mPendingEvents.poll();
                            }

                            WebViewManager.this.mPendingEvents.add(event);
                        }
                    } else {
                        WebViewManager.this.mWebView.evaluateJavascript("window.zapic.dispatch({ type: 'SUBMIT_EVENT', payload: " + event.toString() + " })", null);
                    }

                    return true;
                }
            });
        }
    }

    @MainThread
    void finishActivity() {
        if (this.mActivity == null) {
            throw new IllegalStateException("A ZapicActivity has not been created");
        }

        this.mActivity.finish();
    }

    @Nullable
    ZapicActivity getActivity() {
        return this.mActivity;
    }

    @NonNull
    WebView getWebView() {
        WebView webView = this.mWebView;
        assert webView != null : "mWebView is null";
        return webView;
    }

    void login() {
        final int size = this.mFragments.size();
        if (size == 0) {
            return;
        }

        final ZapicFragment fragment = this.mFragments.get(size - 1);
        fragment.login();
    }

    @MainThread
    void loginFailed(@NonNull final String message) {
        if (this.mWebView != null) {
            this.mWebView.evaluateJavascript("window.zapic.dispatch({ type: 'LOGIN_WITH_PLAY_GAME_SERVICES', error: true, payload: '" + message.replace("'", "\\'") + "' })", null);
        }
    }

    @MainThread
    void loginSucceeded(@NonNull final String packageName, @NonNull final String serverAuthCode) {
        if (this.mWebView != null) {
            this.mWebView.evaluateJavascript("window.zapic.dispatch({ type: 'LOGIN_WITH_PLAY_GAME_SERVICES', payload: { authCode: '" + serverAuthCode.replace("'", "\\'") + "', packageName: '" + packageName.replace("'", "\\'") + "' } })", null);
        }
    }

    void logout() {
        final int size = this.mFragments.size();
        if (size == 0) {
            return;
        }

        final ZapicFragment fragment = this.mFragments.get(size - 1);
        fragment.logout();
    }

    @MainThread
    @SuppressWarnings("deprecation")
    void onActivityCreated(@NonNull final ZapicActivity activity) {
        this.mActivity = activity;
        if (this.mWebView == null) {
            if (this.mAsyncTask != null) {
                this.mAsyncTask.cancel(true);
            }

            this.createEventHandler(activity);
            this.createWebView(activity);
            this.mAsyncTask = new AppSourceAsyncTask(AppSourceConfig.getUrl(), activity.getApplicationContext().getCacheDir()).execute();
        } else if (this.mStarted) {
            this.mWebView.evaluateJavascript("window.zapic.dispatch({ type: 'OPEN_PAGE', payload: '" + activity.getPageParameter() + "' })", null);
        }

        this.showLoadingFragment();
    }

    @MainThread
    void onActivityDestroyed(@NonNull final ZapicActivity activity) {
        if (this.mActivity == activity) {
            this.mActivity = null;
            if (this.mFragments.size() == 0) {
                this.mImageUploadCallback = null;

                if (this.mAsyncTask != null) {
                    this.mAsyncTask.cancel(true);
                    this.mAsyncTask = null;
                }

                if (this.mWebView != null) {
                    this.mWebView.destroy();
                    this.mWebView = null;
                    this.mLoaded = false;
                    this.mStarted = false;
                }
            } else if (this.mWebView != null) {
                this.mWebView.evaluateJavascript("window.zapic.dispatch({ type: 'CLOSE_PAGE' })", null);
            }
        }
    }

    @MainThread
    void onActivityStarted() {
        if (this.mWebView == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.mWebView.getSettings().setOffscreenPreRaster(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.mWebView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false);
        }
    }

    @MainThread
    void onActivityStopped() {
        if (this.mWebView == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.mWebView.getSettings().setOffscreenPreRaster(false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.mWebView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, true);
        }
    }

    @MainThread
    @SuppressWarnings("deprecation")
    void onFragmentCreated(@NonNull final ZapicFragment fragment) {
        this.mFragments.add(fragment);
        if (this.mWebView == null) {
            if (this.mAsyncTask != null) {
                this.mAsyncTask.cancel(true);
            }

            this.createEventHandler(fragment.getActivity());
            this.createWebView(fragment.getActivity());
            this.mAsyncTask = new AppSourceAsyncTask(AppSourceConfig.getUrl(), fragment.getActivity().getApplicationContext().getCacheDir()).execute();
        }
    }

    @MainThread
    void onFragmentDestroyed(@NonNull final ZapicFragment fragment) {
        this.mFragments.remove(fragment);
        if (this.mActivity == null && this.mFragments.size() == 0) {
            this.mImageUploadCallback = null;

            if (this.mAsyncTask != null) {
                this.mAsyncTask.cancel(true);
                this.mAsyncTask = null;
            }

            if (this.mWebView != null) {
                this.mWebView.destroy();
                this.mWebView = null;
                this.mLoaded = false;
                this.mStarted = false;
            }
        }
    }

    @MainThread
    private boolean overrideUrl(@NonNull final WebView view, @NonNull final Uri url) {
        final String scheme = url.getScheme();
        final String host = url.getHost();

        if (scheme != null && scheme.equalsIgnoreCase("market")) {
            try {
                // Create a Google Play Store app intent.
                final Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(url);

                // Open the Google Play Store app.
                final Context context = view.getContext();
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                // TODO: Send an error to the JavaScript application.
            }

            // Prevent the WebView from navigating to an invalid URL.
            return true;
        }

        if (scheme != null && scheme.equalsIgnoreCase("tel")) {
            try {
                // Create a phone app intent.
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(url);

                // Open the phone app.
                final Context context = view.getContext();
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                // TODO: Send an error to the JavaScript application.
            }
        }

        if (host != null && (host.equalsIgnoreCase("itunes.apple.com") || host.toLowerCase().endsWith(".itunes.apple.com"))) {
            if (scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                try {
                    // Create a web browser app intent.
                    final Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(url);

                    // Open the web browser app.
                    final Context context = view.getContext();
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    // TODO: Send an error to the JavaScript application.
                }
            }

            // Prevent the WebView from navigating to an external URL.
            return true;
        }

        if (host != null && (host.equalsIgnoreCase("play.google.com") || host.toLowerCase().endsWith(".play.google.com"))) {
            if (scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                try {
                    // Create a web browser app intent.
                    final Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(url);

                    // Open the web browser app.
                    final Context context = view.getContext();
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    // TODO: Send an error to the JavaScript application.
                }
            }

            // Prevent the WebView from navigating to an external URL.
            return true;
        }

        return false;
    }

    @MainThread
    void setLoaded() {
        this.mLoaded = true;
    }

    @MainThread
    void setStarted() {
        assert this.mWebView != null : "mWebView is null";
        this.mStarted = true;

        synchronized (this.mPendingEventsLock) {
            JSONObject event;
            while ((event = this.mPendingEvents.poll()) != null) {
                this.mWebView.evaluateJavascript("window.zapic.dispatch({ type: 'SUBMIT_EVENT', payload: " + event.toString() + " })", null);
            }
        }
    }

    @MainThread
    void showAppFragment() {
        if (this.mActivity == null) {
            throw new IllegalStateException("A ZapicActivity has not been created");
        }

        final FragmentManager fragmentManager = this.mActivity.getSupportFragmentManager();
        final Fragment currentFragment = fragmentManager.findFragmentById(R.id.activity_zapic_container);
        if (currentFragment == null) {
            fragmentManager.beginTransaction().add(R.id.activity_zapic_container, new ZapicAppFragment()).commit();
        } else if (!(currentFragment instanceof ZapicAppFragment)) {
            fragmentManager.beginTransaction().replace(R.id.activity_zapic_container, new ZapicAppFragment()).commit();
        }
    }

    @MainThread
    private void showLoadingFragment() {
        if (this.mActivity == null) {
            throw new IllegalStateException("A ZapicActivity has not been created");
        }

        final FragmentManager fragmentManager = this.mActivity.getSupportFragmentManager();
        final Fragment currentFragment = fragmentManager.findFragmentById(R.id.activity_zapic_container);
        if (currentFragment == null) {
            fragmentManager.beginTransaction().add(R.id.activity_zapic_container, new ZapicLoadingFragment()).commit();
        } else if (!(currentFragment instanceof ZapicLoadingFragment)) {
            fragmentManager.beginTransaction().replace(R.id.activity_zapic_container, new ZapicLoadingFragment()).commit();
        }
    }

    @MainThread
    private void showOfflineFragment() {
        if (this.mActivity == null) {
            throw new IllegalStateException("A ZapicActivity has not been created");
        }

        final FragmentManager fragmentManager = this.mActivity.getSupportFragmentManager();
        final Fragment currentFragment = fragmentManager.findFragmentById(R.id.activity_zapic_container);
        if (currentFragment == null) {
            fragmentManager.beginTransaction().add(R.id.activity_zapic_container, new ZapicOfflineFragment()).commit();
        } else if (!(currentFragment instanceof ZapicOfflineFragment)) {
            fragmentManager.beginTransaction().replace(R.id.activity_zapic_container, new ZapicOfflineFragment()).commit();
        }
    }

    @MainThread
    void submitImageUpload(@NonNull final Uri[] files) {
        if (this.mImageUploadCallback != null) {
            this.mImageUploadCallback.onReceiveValue(files);
            this.mImageUploadCallback = null;
        }
    }

    @AnyThread
    void submitEvent(@NonNull final JSONObject event) {
        if (this.mHandler == null || this.mWebView == null || !this.mStarted) {
            synchronized (this.mPendingEventsLock) {
                if (this.mPendingEvents.size() == 1000) {
                    this.mPendingEvents.poll();
                }

                this.mPendingEvents.add(event);
            }
        } else {
            this.mHandler.obtainMessage(0, event).sendToTarget();
        }
    }

    @MainThread
    @SuppressWarnings("deprecation")
    void submitLoadApp(@NonNull final AppSource appSource) {
        if (this.mWebView != null) {
            final String url = AppSourceConfig.getUrl();
            this.mWebView.loadDataWithBaseURL(url, appSource.getHtml(), "text/html", "utf-8", url);
        }
    }
}
