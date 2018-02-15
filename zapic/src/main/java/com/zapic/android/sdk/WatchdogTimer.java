package com.zapic.android.sdk;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * A watchdog timer that handles timeout events on a background thread.
 *
 * @author Kyle Dodson
 * @since 1.0.0
 */
final class WatchdogTimer {
    /**
     * The tag used to identify log entries.
     */
    @NonNull
    private static final String TAG = "WatchdogTimer";

    /**
     * The background thread name.
     */
    @NonNull
    private static final String THREAD_NAME = "ZapicWatchdogTimerThread";

    /**
     * The callback that handles timeout events on the background thread when the watchdog timer has
     * elapsed.
     */
    @NonNull
    private final WatchdogTimer.Callback callback;

    /**
     * The handler that sends timeout events on the background thread when the watchdog timer has
     * elapsed.
     */
    @Nullable
    private Handler handler;

    /**
     * The background thread.
     */
    @Nullable
    private HandlerThread handlerThread;

    /**
     * The synchronization lock for {@see handler}, {@see handlerThread}, and {@see messageId}.
     */
    @NonNull
    private final Object lock;

    /**
     * The message sequence identifier.
     */
    private int messageSequenceId;

    /**
     * Creates a new instance.
     *
     * @param callback The callback that handles timeout events on the background thread when the
     *                 watchdog timer has elapsed.
     */
    WatchdogTimer(@NonNull final WatchdogTimer.Callback callback) {
        this.callback = callback;
        this.handler = null;
        this.handlerThread = null;
        this.lock = new Object();
        this.messageSequenceId = 0;
    }

    /**
     * Starts (or resets) the watchdog timer.
     *
     * @param timeout  The timeout (in milliseconds).
     */
    void start(final long timeout) {
        synchronized (this.lock) {
            if (this.handlerThread == null) {
                this.handlerThread = new HandlerThread(WatchdogTimer.THREAD_NAME);
                this.handlerThread.start();
            }

            if (this.handler == null) {
                this.handler = new Handler(this.handlerThread.getLooper(), new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                        boolean elapsed;
                        synchronized (WatchdogTimer.this.lock) {
                            elapsed = msg.arg1 == WatchdogTimer.this.messageSequenceId;
                        }

                        if (elapsed) {
                            Log.d(TAG, "Firing timeout event; the watchdog timer has elapsed");
                            WatchdogTimer.this.callback.handleTimeout();
                        }

                        return true;
                    }
                });
            }

            if (this.messageSequenceId == Integer.MAX_VALUE) {
                this.messageSequenceId = 0;
            }

            this.handler.removeCallbacksAndMessages(null);
            this.handler.sendMessageDelayed(this.handler.obtainMessage(0, ++this.messageSequenceId, 0), timeout);
        }
    }

    /**
     * Stops the watchdog timer.
     */
    void stop() {
        synchronized (this.lock) {
            if (this.handlerThread != null) {
                this.handlerThread.quit();
                this.handlerThread = null;
            }

            if (this.handler != null) {
                this.handler = null;
            }

            this.messageSequenceId = 0;
        }
    }

    /**
     * A callback interface.
     */
    interface Callback {
        /**
         * Handles timeout events on the background thread when the watchdog timer has elapsed.
         */
        void handleTimeout();
    }
}
