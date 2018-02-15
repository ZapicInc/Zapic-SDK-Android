package com.zapic.android.sdk;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * A communication bridge that dispatches messages from the Android application's Java context to
 * the web client application's JavaScript context (running in an Android
 * {@see android.webkit.WebView}).
 *
 * @author Kyle Dodson
 * @since 1.0.0
 */
final class AppJavaBridge {
    /**
     * The tag used to identify log entries.
     */
    @NonNull
    private static final String TAG = "AppJavaBridge";

    /**
     * The handler that sends messages on the UI thread.
     */
    @NonNull
    private final Handler handler;

    /**
     * The synchronization lock for {@see queue}, {@see queueMessageScheduled}, and {@see webView}.
     */
    @NonNull
    private final Object lock;

    /**
     * The queue of actions.
     */
    @NonNull
    private final ArrayList<String> queue;

    /**
     * A value indicating whether a message has been scheduled.
     */
    private boolean queueMessageScheduled;

    /**
     * The Android {@see android.webkit.WebView} running the web client application.
     */
    @Nullable
    private WebView webView;

    /**
     * Creates a new instance.
     *
     * @param webView The Android {@see android.webkit.WebView} running the web client application.
     */
    AppJavaBridge(@NonNull final WebView webView) {
        this.handler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                String[] actions;
                WebView webView;
                synchronized (AppJavaBridge.this.lock) {
                    if (AppJavaBridge.this.webView == null) {
                        return true;
                    }

                    actions = AppJavaBridge.this.queue.toArray(new String[AppJavaBridge.this.queue.size()]);
                    webView = AppJavaBridge.this.webView;

                    AppJavaBridge.this.queue.clear();
                    AppJavaBridge.this.queueMessageScheduled = false;
                }

                if (actions.length == 1) {
                    webView.evaluateJavascript(actions[0], null);
                } else if (actions.length > 1) {
                    webView.evaluateJavascript(TextUtils.join(";", actions), null);
                }

                return true;
            }
        });
        this.lock = new Object();
        this.queue = new ArrayList<>();
        this.queueMessageScheduled = false;
        this.webView = webView;
    }

    void dispatch(@NonNull final String action) {
        try {
            this.send(new JSONObject()
                    .put("type", action));
        } catch (JSONException ignored) {
            // JSONException is only thrown if the value is a non-finite number.
        }
    }

    void dispatch(String action, JSONObject payload) {
        try {
            this.send(new JSONObject()
                    .put("type", action)
                    .put("payload", payload));
        } catch (JSONException ignored) {
            // JSONException is only thrown if the value is a non-finite number.
        }
    }

    void dispatchError(String action) {
        try {
            this.send(new JSONObject()
                    .put("type", action)
                    .put("error", true));
        } catch (JSONException ignored) {
            // JSONException is only thrown if the value is a non-finite number.
        }
    }

    void dispatchError(String action, String message) {
        try {
            this.send(new JSONObject()
                    .put("type", action)
                    .put("error", true)
                    .put("payload", message));
        } catch (JSONException ignored) {
            // JSONException is only thrown if the value is a non-finite number.
        }
    }

    private void send(JSONObject action) {
        synchronized (this.lock) {
            this.queue.add(String.format("window.zapic.dispatch(%s)", action));

            if (!this.queueMessageScheduled && this.webView != null) {
                this.handler.sendEmptyMessageDelayed(0, 50);
                this.queueMessageScheduled = true;
            }
        }
    }
}
