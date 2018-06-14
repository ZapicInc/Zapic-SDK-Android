package com.zapic.androiddemo;

import android.app.Application;

//import com.onesignal.OSNotificationOpenResult;
//import com.onesignal.OneSignal;

import com.zapic.sdk.android.Zapic;
//import com.zapic.sdk.android.ZapicPlayer;

//import org.json.JSONObject;

//import io.branch.referral.Branch;
//import io.branch.referral.BranchApp;

public class MainApplication extends Application { // BranchApp {
    @Override
    public void onCreate() {
        super.onCreate();

//        // Initialize Branch.
//        final Branch branch = Branch.getAutoInstance(this);

//        // Initialize OneSignal.
//        OneSignal.startInit(this)
//                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
//                .unsubscribeWhenNotificationsAreDisabled(true)
//                .setNotificationOpenedHandler(new OneSignal.NotificationOpenedHandler() {
//                    @Override
//                    public void notificationOpened(OSNotificationOpenResult result) {
//                        JSONObject data = result.notification.payload.additionalData;
//                        if (data != null) {
//                            Zapic.handleInteractionEvent(data);
//                        }
//                    }
//                })
//                .init();

        // Initialize Zapic.
        Zapic.start(this);
//        final ZapicPlayer currentPlayer = Zapic.getCurrentPlayer();
//        if (currentPlayer == null) {
//            branch.logout();
//            OneSignal.deleteTag("zapic_player_token");
//        } else {
//            branch.setIdentity(currentPlayer.getPlayerId());
//            OneSignal.sendTag("zapic_player_token", currentPlayer.getPlayerId());
//        }

//        Zapic.start(new Zapic.AuthenticationHandler() {
//            @Override
//            public void onLogin(@NonNull ZapicPlayer player) {
//                // Associate player with Branch session.
//                branch.setIdentity(player.getPlayerId());
//
//                // Associate player/device with OneSignal for push notifications.
//                OneSignal.sendTag(Zapic.NOTIFICATION_TAG, player.getNotificationToken());
//            }
//
//            @Override
//            public void onLogout(@NonNull ZapicPlayer player) {
//                // Disassociate player from Branch session.
//                branch.logout();
//
//                // Disassociate player/device from OneSignal for push notifications.
//                OneSignal.deleteTag(Zapic.NOTIFICATION_TAG);
//            }
//        });
    }
}
