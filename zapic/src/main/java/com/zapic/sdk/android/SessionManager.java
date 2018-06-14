package com.zapic.sdk.android;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.AnyThread;
import android.support.annotation.CheckResult;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentLinkedQueue;

final class SessionManager {
    /**
     * Identifies the "SUBMIT_EVENT" action type.
     */
    private static final int ACTION_TYPE_SUBMIT_EVENT = 1000;

    /**
     * The message handler used to invoke methods on the UI thread.
     */
    @NonNull
    private final Handler mHandler;

    /**
     * The queue of gameplay and interaction event messages.
     */
    @NonNull
    private final ConcurrentLinkedQueue<JSONObject> mMessages;

    /**
     * The current player.
     */
    @Nullable
    private volatile ZapicPlayer mPlayer;

    /**
     * The authentication handler that is notified after a player has logged in or out.
     */
    @Nullable
    private ZapicPlayerAuthenticationHandler mAuthenticationHandler;

    /**
     * The {@link WebView} instance.
     */
    @Nullable
    private WebView mWebView;

    /**
     * Creates a new {@link SessionManager} instance.
     *
     * @param context Any context object (e.g. the global {@link android.app.Application} or an
     *                {@link android.app.Activity}).
     */
    SessionManager(@NonNull final Context context) {
        mAuthenticationHandler = null;
        mHandler = new Handler(context.getApplicationContext().getMainLooper(), new Handler.Callback() {
            @Override
            @SuppressWarnings("unchecked")
            public boolean handleMessage(@Nullable final Message msg) {
                if (msg == null) {
                    return false;
                }

                switch (msg.what) {
                    case ACTION_TYPE_SUBMIT_EVENT:
                        onSubmitEventHandled();
                        break;
                    default:
                        break;
                }

                return true;
            }
        });
        mMessages = new ConcurrentLinkedQueue<>();
        mPlayer = null;
        mWebView = null;
    }

    /**
     * Dispatches a message to the Zapic web page.
     *
     * @param message The message.
     */
    @MainThread
    private void dispatch(@NonNull final JSONObject message) {
        assert mWebView != null : "mWebView == null";
        mWebView.evaluateJavascript("window.zapic.dispatch(" + message.toString() + ")", null);
    }

    /**
     * Dispatches a "SUBMIT_EVENT" message to the Zapic web page.
     *
     * @param message The gameplay or interaction event.
     */
    @MainThread
    private void dispatchSubmitEvent(@NonNull final JSONObject message) {
        if (mWebView != null) {
            try {
                if ("interaction".equals(message.getString("type"))) {
                    final String payload = message.getJSONObject("params").getString("zapic");
                    dispatch(new JSONObject()
                            .put("type", "SUBMIT_EVENT")
                            .put("payload", new JSONObject()
                                    .put("type", "interaction")
                                    .put("payload", payload)));
                } else {
                    dispatch(new JSONObject()
                            .put("type", "SUBMIT_EVENT")
                            .put("payload", message));
                }
            } catch (JSONException ignored) {
            }
        }
    }

    /**
     * Gets the current player.
     *
     * @return The current player or {@code null} if the current player has not logged in.
     */
    @AnyThread
    @CheckResult
    @Nullable
    ZapicPlayer getCurrentPlayer() {
        return mPlayer;
    }

    /**
     * Handles a gameplay or interaction event by relaying it to the {@link WebView}.
     *
     * @param message The gameplay or interaction event.
     */
    @AnyThread
    void handleEvent(@NonNull final JSONObject message) {
        mMessages.offer(message);
        mHandler.obtainMessage(ACTION_TYPE_SUBMIT_EVENT).sendToTarget();
    }

    /**
     * Sets the current player.
     *
     * @param player The current player.
     */
    @MainThread
    void onLogin(@NonNull final ZapicPlayer player) {
        onLogout();

        mPlayer = player;
        final ZapicPlayerAuthenticationHandler authenticationHandler = mAuthenticationHandler;
        if (authenticationHandler != null) {
            authenticationHandler.onLogin(player);
        }
    }

    /**
     * Resets the current player.
     */
    @MainThread
    void onLogout() {
        final ZapicPlayer previousPlayer = mPlayer;
        mPlayer = null;
        if (previousPlayer != null) {
            final ZapicPlayerAuthenticationHandler authenticationHandler = mAuthenticationHandler;
            if (authenticationHandler != null) {
                authenticationHandler.onLogout(previousPlayer);
            }
        }
    }

    /**
     * @see #handleEvent(JSONObject)
     */
    @MainThread
    private void onSubmitEventHandled() {
        if (mWebView != null) {
            while (true) {
                final JSONObject message = mMessages.poll();
                if (message == null) {
                    break;
                }

                dispatchSubmitEvent(message);
            }
        }
    }

    /**
     * Called when the {@link WebView} instance has crashed.
     */
    @MainThread
    void onWebViewCrashed() {
        mWebView = null;
    }

    /**
     * Called when the {@link WebView} instance has loaded the Zapic web page.
     *
     * @param webView The {@link WebView} instance.
     */
    @MainThread
    void onWebViewLoaded(@NonNull final WebView webView) {
        mWebView = webView;
        onSubmitEventHandled();
    }

    /**
     * Sets the authentication handler that is notified after a player has logged in or out.
     * <p>
     * If the current player has already logged in, the
     * {@link ZapicPlayerAuthenticationHandler#onLogin(ZapicPlayer)} method will be immediately
     * invoked with a {@link ZapicPlayer} instance that represents the current user.
     *
     * @param authenticationHandler The authentication handler that is notified after a player has
     *                              logged in or out.
     */
    @MainThread
    void setPlayerAuthenticationHandler(@Nullable final ZapicPlayerAuthenticationHandler authenticationHandler) {
        mAuthenticationHandler = authenticationHandler;
        if (authenticationHandler != null) {
            final ZapicPlayer player = mPlayer;
            if (player != null) {
                authenticationHandler.onLogin(player);
            }
        }
    }
}
