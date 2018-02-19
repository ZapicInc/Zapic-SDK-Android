package com.zapic.android.sdk;

import android.app.Activity;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.AnyThread;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;

/**
 * Provides {@link ConnectivityManager} utility methods.
 */
final class ConnectivityManagerUtilities {
    /**
     * Prevents creating a new {@link ConnectivityManagerUtilities} instance.
     */
    @AnyThread
    private ConnectivityManagerUtilities() {
    }

    /**
     * Gets a value indicating whether the application has network connectivity.
     * <p>
     * This normally indicates whether the device has network connectivity. However, it may also
     * indicate whether data saver has been enabled.
     *
     * @param connectivityManager The Android device's connectivity manager.
     * @return                    {@code true} if the Android device is connected to a network;
     *                            {@code false} if the Android device is not connected to a network.
     */
    @AnyThread
    @CheckResult
    static boolean isConnected(@NonNull ConnectivityManager connectivityManager) {
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    /**
     * Registers and returns a {@link ConnectivityBroadcastReceiver} using the specified activity's
     * context. This should be unregistered using {@link #unregisterConnectivityBroadcastReceiver}.
     *
     * @param activity The activity.
     * @param listener The network connectivity listener.
     * @return         The {@link ConnectivityBroadcastReceiver}.
     * @see #unregisterConnectivityBroadcastReceiver
     */
    @AnyThread
    @NonNull
    static ConnectivityBroadcastReceiver registerConnectivityBroadcastReceiver(@NonNull final Activity activity, @NonNull final ConnectivityListener listener) {
        final IntentFilter connectivityFilter = new IntentFilter();
        connectivityFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        final ConnectivityBroadcastReceiver broadcastReceiver = new ConnectivityBroadcastReceiver(listener);
        activity.registerReceiver(broadcastReceiver, connectivityFilter);

        return broadcastReceiver;
    }

    /**
     * Unregisters a {@link ConnectivityBroadcastReceiver} using the specified activity's context.
     *
     * @param activity          The activity.
     * @param broadcastReceiver The {@link ConnectivityBroadcastReceiver}.
     */
    @AnyThread
    static void unregisterConnectivityBroadcastReceiver(@NonNull final Activity activity, @NonNull final ConnectivityBroadcastReceiver broadcastReceiver) {
        activity.unregisterReceiver(broadcastReceiver);
    }
}
