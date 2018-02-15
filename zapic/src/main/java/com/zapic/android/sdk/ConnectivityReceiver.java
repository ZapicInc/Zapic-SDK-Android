package com.zapic.android.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;

final class ConnectivityReceiver extends BroadcastReceiver {
    @NonNull
    private final AppManager mAppManager;

    ConnectivityReceiver(@NonNull final AppManager appManager) {
        this.mAppManager = appManager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return;
        }

        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            this.mAppManager.setOnline();
        } else {
            this.mAppManager.setOffline();
        }
    }
}
