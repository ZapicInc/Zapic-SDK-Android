package com.zapic.sdk.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
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
            default:
                break;
        }
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
                byte[] imageBytes = Base64.decode(encodedIcon, Base64.DEFAULT);
                icon = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
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
            default:
                break;
        }

        return true;
    }
}
