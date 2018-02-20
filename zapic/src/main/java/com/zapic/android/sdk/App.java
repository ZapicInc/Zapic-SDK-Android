package com.zapic.android.sdk;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.CheckResult;
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
    interface InteractionListener {
        @MainThread
        void login();

        @MainThread
        void logout();

        @MainThread
        void onStateChanged(final State state);

        @MainThread
        void toast(@NonNull final String message);
    }

    enum State {
        LOADED,
        STARTED,
        READY,
        NOT_LOADED,
        NOT_READY,
    }

    /**
     * A communication bridge that dispatches messages from the Zapic JavaScript application.
     *
     * @author Kyle Dodson
     * @since 1.0.0
     */
    final class AppJavascriptInterface {
        /**
         * Identifies an action that indicates the Zapic JavaScript application has loaded.
         */
        private static final int APP_LOADED = 1;

        /**
         * Identifies an action that indicates the Zapic JavaScript application has started.
         */
        private static final int APP_STARTED = 2;

        /**
         * Identifies an action that indicates the Zapic JavaScript application has requested
         * closing the page.
         */
        private static final int CLOSE_PAGE_REQUESTED = 3;

        /**
         * Identifies an action that indicates the Zapic JavaScript application has requested
         * logging in the player.
         */
        private static final int LOGIN = 4;

        /**
         * Identifies an action that indicates the Zapic JavaScript application has requested
         * logging out the player.
         */
        private static final int LOGOUT = 5;

        /**
         * Identifies an action that indicates the Zapic JavaScript application has requested
         * showing a banner to the player.
         */
        private static final int SHOW_BANNER = 6;

        /**
         * Identifies an action that indicates the Zapic JavaScript application is ready to show a
         * page to the player.
         */
        private static final int SHOW_PAGE = 7;

        /**
         * The handler used to run tasks on the main thread.
         */
        @NonNull
        private final Handler mHandler;

        /**
         * Creates a new {@link AppJavascriptInterface} instance.
         */
        private AppJavascriptInterface() {
            this.mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    switch (msg.what) {
                        case APP_LOADED: {
                            App.this.mLoaded = true;
                            App.this.mInteractionListener.onStateChanged(State.LOADED);
                            break;
                        }
                        case APP_STARTED: {
                            App.this.mStarted = true;
                            App.this.mInteractionListener.onStateChanged(State.STARTED);
                            break;
                        }
                        case CLOSE_PAGE_REQUESTED: {
                            App.this.mReady = false;
                            App.this.mInteractionListener.onStateChanged(State.NOT_READY);
                            break;
                        }
                        case LOGIN: {
                            App.this.mInteractionListener.login();
                            break;
                        }
                        case LOGOUT: {
                            App.this.mInteractionListener.logout();
                            break;
                        }
                        case SHOW_BANNER: {
                            App.this.mInteractionListener.toast((String)msg.obj);
                            break;
                        }
                        case SHOW_PAGE: {
                            App.this.mReady = true;
                            App.this.mInteractionListener.onStateChanged(State.READY);
                            break;
                        }
                        default: {
                            break;
                        }
                    }

                    return true;
                }
            });
        }

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
                if (BuildConfig.DEBUG) {
                    Log.e(App.TAG, "Failed to parse serialized action", e);
                }

                return;
            }

            String type;
            try {
                type = action.getString("type");
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) {
                    Log.e(App.TAG, "The action does not have a type", e);
                }

                return;
            }

            switch (type) {
                case "APP_LOADED": {
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(APP_LOADED));
                    break;
                }
                case "APP_STARTED": {
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(APP_STARTED));
                    break;
                }
                case "CLOSE_PAGE_REQUESTED": {
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(CLOSE_PAGE_REQUESTED));
                    break;
                }
                case "LOGGED_IN": {
                    try {
                        String payload = action.getString("payload");
                        Zapic.setPlayerId(payload);
                    } catch (JSONException ignored) {
                        Zapic.setPlayerId(null);
                    }

                    break;
                }
                case "LOGIN": {
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(LOGIN));
                    break;
                }
                case "LOGOUT": {
                    Zapic.setPlayerId(null);
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(LOGOUT));
                    break;
                }
                case "PAGE_READY": {
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(SHOW_PAGE));
                    break;
                }
                case "SHOW_BANNER": {
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(SHOW_BANNER));
                    break;
                }
                default: {
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
            if (BuildConfig.DEBUG) {
                final boolean crashed = detail.didCrash();
                if (crashed) {
                    Log.e(App.TAG, "The WebView has crashed");
                } else {
                    Log.e(App.TAG, "The WebView has been stopped to reclaim memory");
                }
            }

            App.this.mLoaded = false;
            App.this.mReady = false;
            App.this.mStarted = false;
            App.this.mInteractionListener.onStateChanged(State.NOT_LOADED);
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

            final String host = url.getHost();
            if (host != null && (host.equalsIgnoreCase("itunes.apple.com") || host.toLowerCase().endsWith(".itunes.apple.com"))) {
                if (scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                    // Create a web browser app intent.
                    final Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(url);

                    // Open the web browser app.
                    final Context context = view.getContext();
                    context.startActivity(intent);
                }

                // Prevent the WebView from navigating to an external URL.
                return true;
            }

            if (host != null && (host.equalsIgnoreCase("play.google.com") || host.toLowerCase().endsWith(".play.google.com"))) {
                if (scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                    // Create a web browser app intent.
                    final Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(url);

                    // Open the web browser app.
                    final Context context = view.getContext();
                    context.startActivity(intent);
                }

                // Prevent the WebView from navigating to an external URL.
                return true;
            }

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
     * The interaction events listener.
     */
    @NonNull
    private final InteractionListener mInteractionListener;

    /**
     * A value that indicates whether the Zapic JavaScript application has been loaded.
     */
    private boolean mLoaded;

    /**
     * A value that indicates whether the Zapic JavaScript application page is ready.
     */
    private boolean mReady;

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
     *
     * @param interactionListener The interaction event listener.
     */
    App(@NonNull final InteractionListener interactionListener) {
        this.mAsyncTask = null;
        this.mCacheDir = null;
        this.mConnected = false;
        this.mInteractionListener = interactionListener;
        this.mLoaded = false;
        this.mReady = false;
        this.mStarted = false;
        this.mWebView = null;
    }

    /**
     * Gets a value that indicates whether the Android device has network connectivity.
     *
     * @return {@code true} if the Android device has network connectivity; {@code false} if the
     * Android device does not have network connectivity.
     */
    @CheckResult
    @MainThread
    boolean getConnected() {
        return this.mConnected;
    }

    /**
     * Gets the current state.
     *
     * @return The current state.
     */
    @CheckResult
    @MainThread
    State getState() {
        if (this.mReady) {
            return State.READY;
        } else if (this.mStarted) {
            return State.STARTED;
        } else if (this.mLoaded) {
            return State.LOADED;
        } else {
            return State.NOT_LOADED;
        }
    }

    /**
     * Gets the {@link WebView}.
     *
     * @return The {@link WebView}.
     */
    @CheckResult
    @MainThread
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

    @MainThread
    void loadWebViewCancelled() {
        this.mAsyncTask = null;
    }

    @Override
    public void onConnected() {
        if (this.mConnected) {
            return;
        }

        this.mConnected = true;
        if (this.mAsyncTask == null && this.mCacheDir != null && this.getState() == State.NOT_LOADED) {
            // Start an asynchronous task to get the web client application.
            AppSourceAsyncTask asyncTask = new AppSourceAsyncTask(this, App.APP_URL, this.mCacheDir);
            this.mAsyncTask = asyncTask;
            asyncTask.execute();
        }
    }

    @Override
    public void onDisconnected() {
        if (!this.mConnected) {
            return;
        }

        this.mConnected = false;
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

        this.mLoaded = false;
        this.mReady = false;
        this.mStarted = false;
        this.mInteractionListener.onStateChanged(State.NOT_LOADED);

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
