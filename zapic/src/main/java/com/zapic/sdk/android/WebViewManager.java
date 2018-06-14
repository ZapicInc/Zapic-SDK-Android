package com.zapic.sdk.android;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.MutableContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.CheckResult;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.WorkerThread;
import android.support.v4.content.FileProvider;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SafeBrowsingResponse;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/**
 * Manages the {@link WebView} instance.
 * <p>
 * This is responsible for coordinating the lifecycle of the Zapic web page and the {@link WebView}
 * instance. This downloads and caches the Zapic web page, creates the {@link WebView} instance,
 * loads the Zapic web page into the {@link WebView} instance, and dispatches messages between the
 * native and JavaScript contexts. This also restarts the Zapic web page when a crash is detected.
 *
 * @author Kyle Dodson
 * @since 1.2.0
 */
final class WebViewManager {
    /**
     * Identifies the "APP_STARTED" action type.
     */
    private static final int ACTION_TYPE_APP_STARTED = 1000;

    /**
     * Identifies the "CLOSE_PAGE_REQUESTED" action type.
     */
    private static final int ACTION_TYPE_CLOSE_PAGE_REQUESTED = 1001;

    /**
     * Identifies the "LOGGED_IN" action type.
     */
    private static final int ACTION_TYPE_LOGGED_IN = 1002;

    /**
     * Identifies the "LOGGED_OUT" action type.
     */
    private static final int ACTION_TYPE_LOGGED_OUT = 1003;

    /**
     * Identifies the "PAGE_READY" action type.
     */
    private static final int ACTION_TYPE_PAGE_READY = 1004;

    /**
     * Identifies the "SHOW_BANNER" action type.
     */
    private static final int ACTION_TYPE_SHOW_BANNER = 1005;

    /**
     * Identifies the "SHOW_PAGE" action type.
     */
    private static final int ACTION_TYPE_SHOW_PAGE = 1006;

    /**
     * Identifies the "SHOW_SHARE_MENU" action type.
     */
    private static final int ACTION_TYPE_SHOW_SHARE_MENU = 1007;

    /**
     * The tag used to identify log messages.
     */
    @NonNull
    private static final String TAG = "WebViewManager";

    /**
     * The URL of the Zapic web page with a trailing slash.
     */
    @NonNull
    private static final String URL_WITH_SLASH = "https://app.zapic.net/";

    /**
     * The variable name of the {@link WebViewJavascriptBridge} instance in the JavaScript context.
     */
    @NonNull
    private static final String VARIABLE_NAME = "androidWebView";

    /**
     * The global application context.
     */
    @NonNull
    private final Context mApplicationContext;

    /**
     * The message handler used to invoke methods on the UI thread.
     */
    @NonNull
    private final Handler mHandler;

    /**
     * The {@link SessionManager} instance.
     */
    @NonNull
    private final SessionManager mSessionManager;

    /**
     * The {@link ViewManager} instance.
     */
    @NonNull
    private final ViewManager mViewManager;

    /**
     * The {@link WebViewJavascriptBridge} instance.
     */
    @NonNull
    private final WebViewJavascriptBridge mWebViewJavascriptBridge;

    /**
     * A value indicating whether Safe Browsing has been started.
     * <p>
     * This is {@code null} before the first attempt to start Safe Browsing. This is {@code true} if
     * Safe Browsing has successfully been started. This is {@code false} if Safe Browsing could not
     * be started.
     */
    @Nullable
    private Boolean mSafeBrowsingStarted;

    /**
     * The Zapic web page.
     */
    @Nullable
    private WebPage mWebPage;

    /**
     * The asynchronous task used to download and cache the Zapic web page or to indicate a timeout
     * when starting the Zapic web page.
     */
    @Nullable
    private AsyncTask mWebPageTask;

    /**
     * The {@link WebView} instance.
     */
    @Nullable
    private WebView mWebView;

    /**
     * Creates a new {@link WebViewManager} instance.
     *
     * @param context        Any context object (e.g. the global {@link android.app.Application} or
     *                       an {@link android.app.Activity}).
     * @param sessionManager The {@link SessionManager} instance.
     * @param viewManager    The {@link ViewManager} instance.
     */
    @MainThread
    WebViewManager(@NonNull final Context context, @NonNull final SessionManager sessionManager, @NonNull final ViewManager viewManager) {
        mApplicationContext = context.getApplicationContext();
        mHandler = new Handler(mApplicationContext.getMainLooper(), new Handler.Callback() {
            @Override
            @SuppressWarnings("unchecked")
            public boolean handleMessage(@Nullable final Message msg) {
                if (msg == null) {
                    return false;
                }

                switch (msg.what) {
                    case ACTION_TYPE_APP_STARTED:
                        onAppStartedHandled();
                        break;
                    case ACTION_TYPE_CLOSE_PAGE_REQUESTED:
                        onClosePageRequestedHandled();
                        break;
                    case ACTION_TYPE_LOGGED_IN:
                        onLoggedInHandled((Map<String, Object>) msg.obj);
                        break;
                    case ACTION_TYPE_LOGGED_OUT:
                        onLoggedOutHandled();
                        break;
                    case ACTION_TYPE_PAGE_READY:
                        onPageReadyHandled();
                        break;
                    case ACTION_TYPE_SHOW_BANNER:
                        onShowBannerHandled((Map<String, Object>) msg.obj);
                        break;
                    case ACTION_TYPE_SHOW_PAGE:
                        onShowPageHandled();
                        break;
                    case ACTION_TYPE_SHOW_SHARE_MENU:
                        onShowShareMenuHandled((Map<String, Object>) msg.obj);
                    default:
                        break;
                }

                return true;
            }
        });
        mSafeBrowsingStarted = null;
        mSessionManager = sessionManager;
        mViewManager = viewManager;
        mWebPage = null;
        mWebPageTask = null;
        mWebView = null;
        mWebViewJavascriptBridge = new WebViewJavascriptBridge(new ValueCallback<String>() {
            @Override
            @WorkerThread
            public void onReceiveValue(@Nullable final String value) {
                if (value != null) {
                    onDispatch(value);
                }
            }
        });
    }

//    @MainThread
//    @NonNull
//    private static Bitmap captureScreenshot(@NonNull final WebView webView) {
//        Bitmap bitmap = Bitmap.createBitmap(webView.getWidth(), webView.getHeight(), Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas(bitmap);
//        webView.draw(canvas);
//        return bitmap;
//    }

    /**
     * Handles the "APP_STARTED" action. This notifies the various view components that the Zapic
     * web page has started.
     */
    @WorkerThread
    private void onAppStartedDispatched() {
        mHandler.obtainMessage(ACTION_TYPE_APP_STARTED).sendToTarget();
    }

    /**
     * @see #onAppStartedDispatched()
     */
    @MainThread
    private void onAppStartedHandled() {
        if (mWebView != null) {
            mSessionManager.onWebViewLoaded(mWebView);
            mViewManager.onWebViewLoaded(mWebView);
        }
    }

    /**
     * Handles the "CLOSE_PAGE_REQUESTED" action. This notifies the various view components that the
     * Zapic web page should be hidden.
     */
    @WorkerThread
    private void onClosePageRequestedDispatched() {
        mHandler.obtainMessage(ACTION_TYPE_CLOSE_PAGE_REQUESTED).sendToTarget();
    }

    /**
     * @see #onClosePageRequestedDispatched()
     */
    @MainThread
    private void onClosePageRequestedHandled() {
        mViewManager.hideWebPage();
    }

    /**
     * Handles Zapic web page messages.
     *
     * @param message The Zapic web page message.
     */
    @WorkerThread
    private void onDispatch(@NonNull final String message) {
        final JSONObject action;
        try {
            action = new JSONObject(message);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse Zapic web page message", e);
            return;
        }

        final String type;
        try {
            type = action.getString("type");
        } catch (JSONException e) {
            Log.e(TAG, "The Zapic web page message type is missing", e);
            return;
        }

        switch (type) {
            case "APP_STARTED":
                onAppStartedDispatched();
                break;
            case "CLOSE_PAGE_REQUESTED":
                onClosePageRequestedDispatched();
                break;
            case "LOGGED_IN":
                onLoggedInDispatched(action);
                break;
            case "LOGGED_OUT":
                onLoggedOutDispatched();
                break;
            case "PAGE_READY":
                onPageReadyDispatched();
                break;
            case "SHOW_BANNER":
                onShowBannerDispatched(action);
                break;
            case "SHOW_PAGE":
                onShowPageDispatched();
                break;
            case "SHOW_SHARE_MENU":
                onShowShareMenuDispatched(action);
                break;
            default:
                Log.e(TAG, String.format("The Zapic web page message type is not supported: %s", type));
                break;
        }
    }

    /**
     * Handles the "LOGGED_IN" action. This notifies the session manager that the current player has
     * changed.
     */
    @WorkerThread
    private void onLoggedInDispatched(@NonNull final JSONObject action) {
        String notificationToken;
        String userId;
        try {
            final JSONObject payload = action.getJSONObject("payload");
            notificationToken = payload.getString("notificationToken");
            userId = payload.getString("userId");
        } catch (JSONException e) {
            Log.e(TAG, "The Zapic web page LOGGED_IN message is invalid", e);
            return;
        }

        Map<String, Object> args = new HashMap<>();
        args.put("player", new ZapicPlayer(userId, notificationToken));
        mHandler.obtainMessage(ACTION_TYPE_LOGGED_IN, args).sendToTarget();
    }

    /**
     * @see #onLoggedInDispatched(JSONObject)
     */
    @MainThread
    private void onLoggedInHandled(Map<String, Object> args) {
        mSessionManager.onLogin((ZapicPlayer) args.get("player"));
    }

    /**
     * Handles the "LOGGED_OUT" action. This notifies the session manager that the current player
     * has changed.
     */
    @WorkerThread
    private void onLoggedOutDispatched() {
        mHandler.obtainMessage(ACTION_TYPE_LOGGED_OUT).sendToTarget();
    }

    /**
     * @see #onLoggedOutDispatched()
     */
    @MainThread
    private void onLoggedOutHandled() {
        mSessionManager.onLogout();
    }

    /**
     * Handles the "PAGE_READY" action. This notifies the various view components that the Zapic web
     * page may be shown.
     */
    @WorkerThread
    private void onPageReadyDispatched() {
        mHandler.obtainMessage(ACTION_TYPE_PAGE_READY).sendToTarget();
    }

    /**
     * @see #onPageReadyDispatched()
     */
    @MainThread
    private void onPageReadyHandled() {
        mViewManager.onWebViewReady();
    }

    /**
     * Handles the "SHOW_BANNER" action. This shows a notification message on the topmost activity.
     */
    @WorkerThread
    private void onShowBannerDispatched(@NonNull final JSONObject action) {
        String title;
        String subtitle;
        String encodedIcon;
        JSONObject parameters;
        try {
            final JSONObject payload = action.getJSONObject("payload");
            title = payload.getString("title");
            subtitle = payload.optString("subtitle");
            encodedIcon = payload.optString("icon");
            parameters = payload.optJSONObject("parameters");
        } catch (JSONException e) {
            Log.e(TAG, "The Zapic web page SHOW_BANNER message is invalid", e);
            return;
        }

        if (subtitle.equals("")) {
            subtitle = null;
        }

        Bitmap icon;
        if (encodedIcon.equals("")) {
            icon = null;
        } else {
            try {
                byte[] iconBytes = Base64.decode(encodedIcon, Base64.DEFAULT);
                icon = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "The Zapic web page SHOW_BANNER message icon is invalid", e);
                icon = null;
            }

            if (icon != null) {
                final int size = mApplicationContext.getResources().getDimensionPixelSize(R.dimen.alerter_alert_icn_size);
                icon = Bitmap.createScaledBitmap(icon, size, size, false);
            }
        }

        Map<String, Object> args = new HashMap<>();
        args.put("title", title);
        args.put("subtitle", subtitle);
        args.put("icon", icon);
        args.put("parameters", parameters);
        mHandler.obtainMessage(ACTION_TYPE_SHOW_BANNER, args).sendToTarget();
    }

    /**
     * @see #onShowBannerDispatched(JSONObject)
     */
    @MainThread
    private void onShowBannerHandled(Map<String, Object> args) {
        mViewManager.showNotification(new Notification((String) args.get("title"), (String) args.get("subtitle"), (Bitmap) args.get("icon"), (JSONObject) args.get("parameters")));
    }

    /**
     * Handles the "SHOW_PAGE" action. This shows a notification message on the topmost activity.
     */
    @WorkerThread
    private void onShowPageDispatched() {
        mHandler.obtainMessage(ACTION_TYPE_SHOW_PAGE).sendToTarget();
    }

    /**
     * @see #onShowPageDispatched()
     */
    @MainThread
    private void onShowPageHandled() {
        mViewManager.showWebPage();
    }

    /**
     * Handles the "SHOW_SHARE_MENU" action. This shows an app chooser to share content.
     */
    @WorkerThread
    private void onShowShareMenuDispatched(@NonNull final JSONObject action) {
        String text;
        String url;
        String encodedImage;
        try {
            final JSONObject payload = action.getJSONObject("payload");
            text = payload.optString("text");
            url = payload.optString("url");
            encodedImage = payload.optString("image");
        } catch (JSONException e) {
            Log.e(TAG, "The Zapic web page SHOW_SHARE_MENU message is invalid", e);
            return;
        }

        String message = "";
        if (!"".equals(text)) {
            message = text;
        }

        if (!"".equals(url)) {
            if ("".equals(message)) {
                message = url;
            } else {
                message += " " + url;
            }
        }

        if ("".equals(message)) {
            message = null;
        }

        String imageMimeType;
        Uri imageUri;
        if (!encodedImage.equals("")) {
            byte[] imageBytes = Base64.decode(encodedImage, Base64.DEFAULT);
            try {
                // Get the mime type without allocating memory for the pixels.
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
                imageMimeType = options.outMimeType;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "The Zapic web page SHOW_SHARE_MENU message image is invalid", e);
                imageMimeType = null;
            }

            File imageFile;
            if (imageMimeType != null) {
                final File imageDirectory = new File(mApplicationContext.getCacheDir(), "Zapic" + File.separator + "Share");
                if (!imageDirectory.isDirectory() && !imageDirectory.mkdirs()) {
                    imageFile = null;
                } else {
                    String imageFileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(imageMimeType);
                    if (imageFileExtension == null) {
                        imageFileExtension = "file";
                    }

                    imageFile = new File(imageDirectory, "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + "." + imageFileExtension);
                }
            } else {
                imageFile = null;
            }

            if (imageFile != null) {
                try {
                    BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(imageFile));
                    outputStream.write(imageBytes);
                    outputStream.flush();
                    outputStream.close();

                    final String packageName = mApplicationContext.getPackageName();
                    imageUri = FileProvider.getUriForFile(mApplicationContext, packageName + ".zapic", imageFile);
                } catch (IllegalArgumentException | IOException e) {
                    imageUri = null;
                }
            } else {
                imageUri = null;
            }
        } else {
            imageMimeType = null;
            imageUri = null;
        }

        Intent intent;
        if (imageUri != null) {
            intent = new Intent(Intent.ACTION_SEND);
            intent.setType(imageMimeType);
            intent.putExtra(Intent.EXTRA_STREAM, imageUri);
            if (message != null) {
                intent.putExtra(Intent.EXTRA_TEXT, message);
            }
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else if (message != null) {
            intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, message);
        } else {
            intent = null;
        }

        if (intent != null) {
            Map<String, Object> args = new HashMap<>();
            args.put("intent", intent);
            mHandler.obtainMessage(ACTION_TYPE_SHOW_SHARE_MENU, args).sendToTarget();
        }
    }

    /**
     * @see #onShowShareMenuDispatched(JSONObject)
     */
    @MainThread
    private void onShowShareMenuHandled(@NonNull final Map<String, Object> args) {
        mViewManager.showShareChooser((Intent) args.get("intent"));
    }

    /**
     * Creates the {@link WebView} instance and starts the Zapic web page.
     * <p>
     * This should only be called once.
     */
    @MainThread
    void start() {
        startDownload();
        startSafeBrowsing();
    }

    /**
     * If necessary, starts downloading the Zapic web page in a background thread. This calls
     * {@link #startWebView()} after setting {@link #mWebPage}.
     */
    @MainThread
    private void startDownload() {
        if (mWebPage == null) {
            if (mWebPageTask == null) {
                mViewManager.showLoadingPage();
                mWebPageTask = new WebPageAsyncTask(
                        mApplicationContext,
                        new ValueCallback<WebPage>() {
                            @MainThread
                            @Override
                            public void onReceiveValue(@Nullable final WebPage value) {
                                mViewManager.showLoadingPage();
                                mWebPageTask.cancel(true);
                                mWebPageTask = null;

                                if (value == null) {
                                    mWebPage = null;
                                    startDownload();
                                } else {
                                    mWebPage = value;
                                    startWebView();
                                }
                            }
                        },
                        new ValueCallback<Integer>() {
                            @MainThread
                            @Override
                            public void onReceiveValue(@Nullable final Integer value) {
                                if (value != null && value > 1) {
                                    mViewManager.showRetryPage();
                                }
                            }
                        }).execute();
            }
        } else {
            startWebView();
        }
    }

    /**
     * If necessary, starts Safe Browsing. This calls {@link #startWebView()} after setting
     * {@link #mSafeBrowsingStarted}.
     */
    @MainThread
    private void startSafeBrowsing() {
        if (mSafeBrowsingStarted == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                WebView.startSafeBrowsing(mApplicationContext, new ValueCallback<Boolean>() {
                    @MainThread
                    @Override
                    public void onReceiveValue(@Nullable final Boolean value) {
                        if (value == null) {
                            mSafeBrowsingStarted = false;
                        } else {
                            mSafeBrowsingStarted = value;
                        }

                        startWebView();
                    }
                });
            } else {
                mSafeBrowsingStarted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
                startWebView();
            }
        } else {
            startWebView();
        }
    }

    /**
     * If necessary, creates {@link #mWebView} and starts the Zapic web page.
     */
    @MainThread
    @SuppressLint("SetJavaScriptEnabled")
    private void startWebView() {
        if (mSafeBrowsingStarted == null || mWebPage == null) {
            return;
        }

        if (mWebView == null) {
            mWebView = new WebView(new MutableContextWrapper(mApplicationContext));
            mWebView.setVisibility(View.GONE);
            mWebView.addJavascriptInterface(mWebViewJavascriptBridge, VARIABLE_NAME);
            mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            mWebView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            mWebView.setWebChromeClient(new ChromeClient());
            mWebView.setWebViewClient(new ViewClient());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mWebView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, true);
            }

            WebSettings webSettings = mWebView.getSettings();
            webSettings.setAppCacheEnabled(false);
            webSettings.setBlockNetworkImage(false);
            webSettings.setBlockNetworkLoads(false);
            webSettings.setBuiltInZoomControls(false);
            webSettings.setDatabaseEnabled(false);
            webSettings.setDisplayZoomControls(false);
            webSettings.setDomStorageEnabled(true);
            webSettings.setGeolocationEnabled(false);
            webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
            webSettings.setJavaScriptEnabled(true);
            webSettings.setLoadsImagesAutomatically(true);
            webSettings.setMediaPlaybackRequiresUserGesture(false);
            webSettings.setSaveFormData(false);
            webSettings.setSupportMultipleWindows(false);
            webSettings.setSupportZoom(false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                webSettings.setDisabledActionModeMenuItems(WebSettings.MENU_ITEM_PROCESS_TEXT | WebSettings.MENU_ITEM_SHARE | WebSettings.MENU_ITEM_WEB_SEARCH);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                webSettings.setSafeBrowsingEnabled(mSafeBrowsingStarted != null ? mSafeBrowsingStarted : false);
            }
        }

        if (mWebPage == null) {
            mWebView.loadUrl("about:blank");
        } else {
            mWebView.loadUrl(URL_WITH_SLASH);
        }
    }

    private final class ChromeClient extends WebChromeClient {
        @Override
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            String[] acceptTypes = fileChooserParams.getAcceptTypes();
            if (acceptTypes == null || acceptTypes.length != 1 || !acceptTypes[0].equalsIgnoreCase("image/*") || fileChooserParams.getMode() != FileChooserParams.MODE_OPEN) {
                return false;
            }

            mViewManager.showImageChooser(filePathCallback);
            return true;
        }
    }

    private final class ViewClient extends WebViewClient {
        @Override
        @RequiresApi(Build.VERSION_CODES.O)
        public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
            if (mWebView == null || mWebView != view) {
                return false;
            }

            mSessionManager.onWebViewCrashed();
            mViewManager.onWebViewCrashed();
            view.destroy();

            final boolean crashed = detail.didCrash();
            if (crashed) {
                Log.e(TAG, "The Zapic web page has crashed");
            } else {
                Log.e(TAG, "The Zapic web page has stopped to free memory");
            }

            if (mWebPageTask != null) {
                mWebPageTask.cancel(true);
            }

            mWebPage = null;
            mWebPageTask = null;
            mWebView = null;

            startDownload();
            startSafeBrowsing();
            return true;
        }

        @Override
        @RequiresApi(Build.VERSION_CODES.O_MR1)
        public void onSafeBrowsingHit(WebView view, WebResourceRequest request, int threatType, SafeBrowsingResponse callback) {
            final String url = request.getUrl().toString();
            if (url.startsWith(URL_WITH_SLASH)) {
                callback.proceed(false);
            } else {
                super.onSafeBrowsingHit(view, request, threatType, callback);
            }
        }

        @Nullable
        @Override
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        public WebResourceResponse shouldInterceptRequest(final WebView view, final WebResourceRequest request) {
            return shouldInterceptRequestImpl(request.getMethod(), request.getUrl().toString());
        }

        @Nullable
        @Override
        @SuppressWarnings("deprecation")
        public WebResourceResponse shouldInterceptRequest(final WebView view, final String url) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                return shouldInterceptRequestImpl("GET", url);
            } else {
                return null;
            }
        }

        @CheckResult
        @Nullable
        private WebResourceResponse shouldInterceptRequestImpl(@NonNull final String method, @NonNull final String url) {
            if ("GET".equalsIgnoreCase(method) && (url.startsWith(URL_WITH_SLASH) && !url.startsWith(URL_WITH_SLASH + "api/"))) {
                InputStream data;
                Map<String, String> headers;
                String reasonPhrase;
                int statusCode;
                if (mWebPage == null) {
                    data = new ByteArrayInputStream(new byte[0]);
                    headers = new HashMap<>();
                    reasonPhrase = "Not Found";
                    statusCode = HttpsURLConnection.HTTP_NOT_FOUND;
                } else {
                    data = new ByteArrayInputStream(mWebPage.getHtml().getBytes(StandardCharsets.UTF_8));
                    headers = mWebPage.getHeaders();
                    reasonPhrase = "OK";
                    statusCode = HttpsURLConnection.HTTP_OK;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    return new WebResourceResponse("text/html", "utf-8", statusCode, reasonPhrase, headers, data);
                } else {
                    return new WebResourceResponse("text/html", "utf-8", data);
                }
            }

            return null;
        }

        @Override
        @RequiresApi(Build.VERSION_CODES.N)
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return shouldOverrideUrlLoadingImpl(view, request.getMethod(), request.getUrl());
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP || shouldOverrideUrlLoadingImpl(view, "GET", Uri.parse(url));
        }

        @CheckResult
        private boolean shouldOverrideUrlLoadingImpl(@NonNull final WebView view, @NonNull final String method, @NonNull final Uri uri) {
            final String url = uri.toString();
            if ("GET".equalsIgnoreCase(method) && (url.startsWith(URL_WITH_SLASH) && !url.startsWith(URL_WITH_SLASH + "api/"))) {
                // Allow the WebView to navigate to the Zapic web page.
                return false;
            }

            final String scheme = uri.getScheme();
            if (scheme != null && scheme.equalsIgnoreCase("mailto")) {
                try {
                    // Create a phone app intent.
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(uri);

                    // Open the email app.
                    final Context context = view.getContext();
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "mailto: link not supported", e);
                }
            } else if (scheme != null && scheme.equalsIgnoreCase("tel")) {
                try {
                    // Create a phone app intent.
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(uri);

                    // Open the phone app.
                    final Context context = view.getContext();
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "tel: link not supported", e);
                }
            } else {
                try {
                    // Create a generic app intent (Chrome, Google Play Store, etc.).
                    final Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(uri);

                    // Open the app.
                    final Context context = view.getContext();
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, String.format("%s: link not supported", scheme), e);
                }
            }

            // Prevent the WebView from navigating to an invalid URL.
            return true;
        }
    }
}
