package com.zapic.android.sdk;

import android.support.annotation.MainThread;

/**
 * Represents a network connectivity listener.
 */
interface ConnectivityListener {
    /**
     * Called when the application gains network connectivity.
     * <p>
     * This normally indicates the device gained network connectivity. However, it may also indicate
     * data saver has been disabled.
     */
    @MainThread
    void onConnected();

    /**
     * Called when the application loses network connectivity.
     * <p>
     * This normally indicates the device lost network connectivity. However, it may also indicate
     * data saver has been enabled.
     */
    @MainThread
    void onDisconnected();
}
