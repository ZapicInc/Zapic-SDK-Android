package com.zapic.androiddemo;

import android.support.annotation.NonNull;

import com.onesignal.OSNotificationOpenResult;
import com.onesignal.OneSignal;
import com.zapic.sdk.android.Zapic;
import com.zapic.sdk.android.ZapicPlayer;
import com.zapic.sdk.android.ZapicPlayerAuthenticationHandler;

import org.json.JSONObject;

import io.branch.referral.Branch;
import io.branch.referral.BranchApp;

public class MainApplication extends BranchApp {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Branch.
        final Branch branch = Branch.getAutoInstance(this);

        // Initialize OneSignal.
        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .setNotificationOpenedHandler(new OneSignal.NotificationOpenedHandler() {
                    @Override
                    public void notificationOpened(OSNotificationOpenResult result) {
                        JSONObject data = result.notification.payload.additionalData;
                        if (data != null) {
                            Zapic.handleInteraction(data);
                        }
                    }
                })
                .init();

        // Initialize Zapic.
        Zapic.start(this);
        Zapic.setPlayerAuthenticationHandler(new ZapicPlayerAuthenticationHandler() {
            @Override
            public void onLogin(@NonNull ZapicPlayer player) {
                // Associate player with Branch session.
                branch.setIdentity(player.getPlayerId());

                // Associate player/device with OneSignal for push notifications.
                OneSignal.sendTag("zapic_player_token", player.getNotificationToken());
            }

            @Override
            public void onLogout(@NonNull ZapicPlayer player) {
                // Disassociate player from Branch session.
                branch.logout();

                // Disassociate player/device from OneSignal for push notifications.
                OneSignal.deleteTag("zapic_player_token");
            }
        });
    }
}
