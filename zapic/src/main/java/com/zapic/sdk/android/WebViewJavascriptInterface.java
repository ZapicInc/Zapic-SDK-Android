package com.zapic.sdk.android;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.content.FileProvider;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class WebViewJavascriptInterface implements Callback {
    /**
     * Identifies the "APP_LOADED" action type.
     */
    private static final int APP_LOADED = 1000;

    /**
     * Identifies the "APP_STARTED" action type.
     */
    private static final int APP_STARTED = 1001;

    /**
     * Identifies the "CLOSE_PAGE_REQUESTED" action type.
     */
    private static final int CLOSE_PAGE_REQUESTED = 1002;

    /**
     * Identifies the "LOGIN" action type.
     */
    private static final int LOGIN = 1003;

    /**
     * Identifies the "LOGOUT" action type.
     */
    private static final int LOGOUT = 1004;

    /**
     * Identifies the "PAGE_READY" action type.
     */
    private static final int PAGE_READY = 1005;

    /**
     * Identifies the "SHOW_BANNER" action type.
     */
    private static final int SHOW_BANNER = 1006;

    /**
     * Identifies the "SHOW_SHARE_MENU" action type.
     */
    private static final int SHOW_SHARE_MENU = 1007;

    /**
     * The tag used to identify log messages.
     */
    @NonNull
    private static final String TAG = "WebViewJavascriptInterf";

    /**
     * The application context.
     */
    @NonNull
    private final Context mContext;

    /**
     * The handler used to dispatch messages to the main thread.
     */
    @NonNull
    private final Handler mHandler;

    /**
     * Creates a new {@link WebViewJavascriptInterface} instance.
     *
     * @param context The context from which to obtain the main looper.
     */
    WebViewJavascriptInterface(@NonNull final Context context) {
        this.mContext = context.getApplicationContext();
        this.mHandler = new Handler(context.getApplicationContext().getMainLooper(), this);
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
                Log.e(TAG, "Failed to parse serialized action", e);
            }

            return;
        }

        String type;
        try {
            type = action.getString("type");
        } catch (JSONException e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "The action does not have a type", e);
            }

            return;
        }

        switch (type) {
            case "APP_LOADED":
                this.onAppLoadedDispatched();
                break;
            case "APP_STARTED":
                this.onAppStartedDispatched();
                break;
            case "CLOSE_PAGE_REQUESTED":
                this.onClosePageRequestedDispatched();
                break;
            case "LOGGED_IN":
                this.onLoggedInDispatched(action);
                break;
            case "LOGGED_OUT":
                this.onLoggedOutDispatched();
                break;
            case "LOGIN":
                this.onLoginDispatched();
                break;
            case "LOGOUT":
                this.onLogoutDispatched();
                break;
            case "PAGE_READY":
                this.onPageReadyDispatched();
                break;
            case "SHOW_BANNER":
                this.onShowBannerDispatched(action);
                break;
            case "SHOW_SHARE_MENU":
                this.onShowShareMenuDispatched(action);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean handleMessage(@Nullable final Message msg) {
        if (msg == null) {
            return false;
        }

        @SuppressWarnings("unchecked") final Map<String, Object> args = (Map<String, Object>) msg.obj;
        switch (msg.what) {
            case APP_LOADED:
                this.onAppLoadedHandled();
                break;
            case APP_STARTED:
                this.onAppStartedHandled();
                break;
            case CLOSE_PAGE_REQUESTED:
                this.onClosePageRequestedHandled();
                break;
            case LOGIN:
                this.onLoginHandled();
                break;
            case LOGOUT:
                this.onLogoutHandled();
                break;
            case PAGE_READY:
                this.onPageReadyHandled();
                break;
            case SHOW_BANNER:
                this.onShowBannerHandled(args);
                break;
            case SHOW_SHARE_MENU:
                this.onShowShareMenuHandled(args);
            default:
                break;
        }

        return true;
    }

    @WorkerThread
    private void onAppLoadedDispatched() {
        this.mHandler.obtainMessage(APP_LOADED).sendToTarget();
    }

    @MainThread
    private void onAppLoadedHandled() {
        WebViewManager.getInstance().setLoaded();
    }

    @WorkerThread
    private void onAppStartedDispatched() {
        this.mHandler.obtainMessage(APP_STARTED).sendToTarget();
    }

    @MainThread
    private void onAppStartedHandled() {
        final WebViewManager webViewManager = WebViewManager.getInstance();
        webViewManager.setStarted();

        final ZapicActivity activity = webViewManager.getActivity();
        if (activity == null) {
            return;
        }

        final WebView webView = webViewManager.getWebView();
        webView.evaluateJavascript("window.zapic.dispatch({ type: 'OPEN_PAGE', payload: '" + activity.getPageParameter() + "' })", null);
    }

    @WorkerThread
    private void onClosePageRequestedDispatched() {
        this.mHandler.obtainMessage(CLOSE_PAGE_REQUESTED).sendToTarget();
    }

    @MainThread
    private void onClosePageRequestedHandled() {
        WebViewManager.getInstance().finishActivity();
    }

    @WorkerThread
    private void onLoggedInDispatched(@NonNull final JSONObject action) {
        String userId;
        try {
            final JSONObject payload = action.getJSONObject("payload");
            userId = payload.getString("userId");
        } catch (JSONException ignored) {
            // TODO: Send an error to the JavaScript application.
            return;
        }

        if (userId.equals("")) {
            // TODO: Send an error to the JavaScript application.
            return;
        }

        Zapic.setPlayerId(userId);
    }

    @WorkerThread
    private void onLoggedOutDispatched() {
        Zapic.setPlayerId(null);
    }

    @WorkerThread
    private void onLoginDispatched() {
        this.mHandler.obtainMessage(LOGIN).sendToTarget();
    }

    @MainThread
    private void onLoginHandled() {
        WebViewManager.getInstance().login();
    }

    @WorkerThread
    private void onLogoutDispatched() {
        this.mHandler.obtainMessage(LOGOUT).sendToTarget();
    }

    @MainThread
    private void onLogoutHandled() {
        WebViewManager.getInstance().logout();
    }

    @WorkerThread
    private void onPageReadyDispatched() {
        this.mHandler.obtainMessage(PAGE_READY).sendToTarget();
    }

    @MainThread
    private void onPageReadyHandled() {
        WebViewManager.getInstance().showAppFragment();
    }

    @WorkerThread
    private void onShowBannerDispatched(@NonNull final JSONObject action) {
        String title;
        String subtitle;
        String encodedIcon;
        try {
            final JSONObject payload = action.getJSONObject("payload");
            title = payload.getString("title");
            subtitle = payload.optString("subtitle");
            encodedIcon = payload.optString("icon");
        } catch (JSONException e) {
            // TODO: Send an error to the JavaScript application.
            return;
        }

        if (title.equals("")) {
            // TODO: Send an error to the JavaScript application.
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
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Failed to parse icon", e);
                }

                icon = null;
            }

            if (icon != null) {
                final int size = this.mContext.getResources().getDimensionPixelSize(R.dimen.component_zapic_toast_icon_size);
                icon = Bitmap.createScaledBitmap(icon, size, size, false);
            }
        }

        Map<String, Object> args = new HashMap<>();
        args.put("title", title);
        args.put("subtitle", subtitle);
        args.put("icon", icon);
        this.mHandler.obtainMessage(SHOW_BANNER, args).sendToTarget();
    }

    @MainThread
    private void onShowBannerHandled(Map<String, Object> args) {
        NotificationUtilities.showBanner(this.mContext, (String) args.get("title"), (String) args.get("subtitle"), (Bitmap) args.get("icon"));
    }

    @WorkerThread
    private void onShowShareMenuDispatched(@NonNull final JSONObject action) {
        Map<String, Object> args = new HashMap<>();
        String text;
        String url;
        String encodedImage;
        try {
            final JSONObject payload = action.getJSONObject("payload");
            text = payload.optString("text");
            url = payload.optString("url");
            encodedImage = payload.optString("image");
        } catch (JSONException e) {
            // TODO: Send an error to the JavaScript application.
            return;
        }

        if (!text.equals("")) {
            args.put("text", text);
        }

        if (!url.equals("")) {
            args.put("url", url);
        }

        if (!encodedImage.equals("")) {
            byte[] imageBytes = Base64.decode(encodedImage, Base64.DEFAULT);
            String imageMimeType;
            try {
                // Get the mime type without allocating memory for the pixels.
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
                imageMimeType = options.outMimeType;
            } catch (IllegalArgumentException e) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Failed to parse image", e);
                }

                imageMimeType = null;
            }

            File imageFile;
            if (imageMimeType != null) {
                final File filesDir = this.mContext.getFilesDir();
                if (filesDir == null) {
                    imageFile = null;
                } else {
                    final File zapicDir = new File(filesDir.getAbsolutePath() + File.separator + "Zapic");
                    if (!zapicDir.isDirectory() && !zapicDir.mkdirs()) {
                        imageFile = null;
                    } else {
                        String imageFileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(imageMimeType);
                        if (imageFileExtension == null) {
                            imageFileExtension = "file";
                        }

                        imageFile = new File(zapicDir.getAbsolutePath() + File.separator + "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + "." + imageFileExtension);
                    }
                }
            } else {
                imageFile = null;
            }

            Uri imageUri;
            if (imageFile != null) {
                try {
                    BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(imageFile));
                    outputStream.write(imageBytes);
                    outputStream.flush();
                    outputStream.close();

                    final String packageName = this.mContext.getPackageName();
                    imageUri = FileProvider.getUriForFile(this.mContext, packageName + ".zapic", imageFile);
                } catch (IllegalArgumentException | IOException e) {
                    imageUri = null;
                }
            } else {
                imageUri = null;
            }

            if (imageUri != null) {
                args.put("imageMimeType", imageMimeType);
                args.put("imageUri", imageUri);
            }
        }

        if (args.size() > 0) {
            this.mHandler.obtainMessage(SHOW_SHARE_MENU, args).sendToTarget();
        }
    }

    @MainThread
    private void onShowShareMenuHandled(Map<String, Object> args) {
        final WebViewManager webViewManager = WebViewManager.getInstance();
        final ZapicActivity activity = webViewManager.getActivity();
        if (activity == null) {
            return;
        }

        final String text = args.containsKey("text") ? (String) args.get("text") : null;
        final String url = args.containsKey("url") ? (String) args.get("url") : null;
        final String message = text != null && url != null
                ? text + " " + url
                : text != null ? text : url;

        final String imageMimeType = args.containsKey("imageMimeType") ? (String) args.get("imageMimeType") : null;
        final Uri imageUri = args.containsKey("imageUri") ? (Uri) args.get("imageUri") : null;

        Intent intent;
        if (imageUri != null && imageMimeType != null) {
            intent = new Intent(Intent.ACTION_SEND);
            intent.setType(imageMimeType);
            intent.putExtra(Intent.EXTRA_STREAM, imageUri);
            if (message != null) {
                intent.putExtra(Intent.EXTRA_TEXT, message);
            }
        } else if (message != null) {
            intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, message);
        } else {
            intent = null;
        }

        if (intent != null) {
            activity.startActivity(Intent.createChooser(intent, "Share"));
        }
    }
}
