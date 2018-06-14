package com.zapic.sdk.android;

import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An object injected into the Zapic web page. This provides the JavaScript context a
 * {@code dispatch} method to pass Flux Standard Action messages to the native SDK.
 *
 * @author Kyle Dodson
 * @since 1.2.0
 */
final class WebViewJavascriptBridge {
    /**
     * The tag used to identify log messages.
     */
    @NonNull
    private static final String TAG = "WebViewJavascriptBridge";

    /**
     * The executor service used to dispatch messages from the Zapic web page on a thread pool
     * thread.
     */
    @NonNull
    private final ExecutorService mExecutorService;

    /**
     * The callback invoked after receiving a Zapic web page message. This may be invoked multiple
     * times.
     */
    @NonNull
    private final ValueCallback<String> mMessageCallback;

    /**
     * Creates a new {@link WebViewJavascriptBridge} instance.
     *
     * @param messageCallback The callback invoked after receiving a Zapic web page message. This
     *                        may be invoked multiple times.
     */
    WebViewJavascriptBridge(@NonNull final ValueCallback<String> messageCallback) {
        mExecutorService = Executors.unconfigurableExecutorService(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), Executors.defaultThreadFactory()));
        mMessageCallback = messageCallback;
    }

    /**
     * Dispatches a message from the Zapic web page.
     *
     * @param message The message.
     */
    @JavascriptInterface
    @WorkerThread
    public void dispatch(@Nullable final String message) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, String.format("Received Zapic web page message: %s", message));
        }

        if (message == null) {
            return;
        }

        mExecutorService.execute(new DecodeAndProcessMessageRunnable(message));
    }

    /**
     * A runnable task that dispatches a Zapic web page message on a thread pool thread.
     */
    private final class DecodeAndProcessMessageRunnable implements Runnable {
        /**
         * The message.
         */
        @NonNull
        private final String mMessage;

        /**
         * Creates a new {@link DecodeAndProcessMessageRunnable} instance.
         *
         * @param message The message.
         */
        @AnyThread
        private DecodeAndProcessMessageRunnable(@NonNull final String message) {
            mMessage = message;
        }

        @Override
        @WorkerThread
        public void run() {
            mMessageCallback.onReceiveValue(mMessage);
        }
    }
}
