package com.zapic.sdk.android;

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
    private final String notificationToken;

    /**
     * The player identifier.
     */
    @NonNull
    private final String playerId;

    /**
     * Creates a new {@link ZapicPlayer} instance.
     */
    ZapicPlayer(@NonNull String playerId, @NonNull String notificationToken) {
        this.playerId = playerId;
        this.notificationToken = notificationToken;
    }

    /**
     * Gets the notification token.
     *
     * @return The notification token.
     */
    @CheckResult
    @NonNull
    @SuppressWarnings("unused")
    public String getNotificationToken() {
        return notificationToken;
    }

    /**
     * Gets the player identifier.
     *
     * @return The player identifier.
     */
    @CheckResult
    @NonNull
    @SuppressWarnings("unused")
    public String getPlayerId() {
        return playerId;
    }
}
