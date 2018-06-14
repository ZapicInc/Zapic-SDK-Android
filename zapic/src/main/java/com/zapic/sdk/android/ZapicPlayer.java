package com.zapic.sdk.android;

import android.support.annotation.AnyThread;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;

/**
 * Represents a Zapic player.
 *
 * @author Kyle Dodson
 * @since 1.0.2
 */
public final class ZapicPlayer {
    /**
     * The notification token.
     */
    @NonNull
    private final String mNotificationToken;

    /**
     * The player identifier.
     */
    @NonNull
    private final String mPlayerId;

    /**
     * Creates a new {@link ZapicPlayer} instance.
     *
     * @param playerId          The player identifier.
     * @param notificationToken The notification token.
     */
    @AnyThread
    ZapicPlayer(@NonNull final String playerId, @NonNull final String notificationToken) {
        mNotificationToken = notificationToken;
        mPlayerId = playerId;
    }

    /**
     * Gets the notification token.
     *
     * @return The notification token.
     */
    @AnyThread
    @CheckResult
    @NonNull
    @SuppressWarnings({"unused", "WeakerAccess"})
    public String getNotificationToken() {
        return mNotificationToken;
    }

    /**
     * Gets the player identifier.
     *
     * @return The player identifier.
     */
    @AnyThread
    @CheckResult
    @NonNull
    @SuppressWarnings({"unused", "WeakerAccess"})
    public String getPlayerId() {
        return mPlayerId;
    }
}
