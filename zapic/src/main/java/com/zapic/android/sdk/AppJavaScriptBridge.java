package com.zapic.android.sdk;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.JavascriptInterface;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A communication bridge that dispatches messages from the web client application's JavaScript
 * context (running in an Android {@see android.webkit.WebView}) to the Android application's Java
 * context.
 *
 * @author Kyle Dodson
 * @since 1.0.0
 */
final class AppJavaScriptBridge {
    /**
     * The tag used to identify log entries.
     */
    @NonNull
    private static final String TAG = "AppJavaScriptBridge";

    /**
     * Dispatches a message from the web client application's JavaScript context to the Android
     * application's Java context.
     * <p>
     * This is <i>not</i> invoked on the UI thread.
     *
     * @param message The message.
     */
    @JavascriptInterface
    public void dispatch(@Nullable final String message) {
        JSONObject action;
        try {
            action = new JSONObject(message);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse serialized action", e);
            return;
        }

        String type;
        try {
            type = action.getString("type");
        } catch (JSONException e) {
            Log.e(TAG, "The action does not have a type", e);
            return;
        }

        switch (type) {
            case "APP_LOADED":
            case "LOGIN":
            case "LOGGED_IN":
            case "SHOW_BANNER":
            case "APP_STARTED":
            case "CLOSE_PAGE_REQUESTED":
                Log.d(TAG, message);
                break;
            default:
                Log.e(TAG, String.format("The action type is invalid: %s", type));
                break;
        }
    }
}
