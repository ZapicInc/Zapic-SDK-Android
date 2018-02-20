package com.zapic.android.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.support.annotation.AnyThread;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;

/**
 * A {@link ConnectivityManager} broadcast receiver that relays network connectivity changes.
 *
 * @author Kyle Dodson
 * @since 1.0.0
 */
final class ConnectivityBroadcastReceiver extends BroadcastReceiver {
    /**
     * The {@link ConnectivityListener} to notify on network connectivity changes.
     */
    @NonNull
    private final ConnectivityListener mConnectivityListener;

    /**
     * Creates a new {@link ConnectivityBroadcastReceiver} instance.
     *
     * @param connectivityListener The {@link ConnectivityListener} to notify on network
     *                             connectivity changes.
     */
    @AnyThread
    ConnectivityBroadcastReceiver(@NonNull final ConnectivityListener connectivityListener) {
        this.mConnectivityListener = connectivityListener;
    }

    /**
     * Notifies the listener of connectivity changes.
     *
     * @param context The application context or an activity context.
     */
    @MainThread
    void notify(@NonNull final Context context) {
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return;
        }

        if (ConnectivityManagerUtilities.isConnected(connectivityManager)) {
            this.mConnectivityListener.onConnected();
        } else {
            this.mConnectivityListener.onDisconnected();
        }
    }

    @MainThread
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        this.notify(context);
    }
}
