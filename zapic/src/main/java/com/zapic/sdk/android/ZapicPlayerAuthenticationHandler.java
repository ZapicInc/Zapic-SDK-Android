package com.zapic.sdk.android;

import android.support.annotation.MainThread;
import android.support.annotation.NonNull;

/**
 * Represents an authentication handler that is notified after a player has logged in or out.
 *
 * @author Kyle Dodson
 * @since 1.2.0
 */
public interface ZapicPlayerAuthenticationHandler {
    /**
     * Invoked after the player has logged in.
     * <p>
     * The game may use this callback to enable features reserved for authenticated players. The
     * game may also use this callback to load game data specific to the player.
     *
     * @param player The current player.
     */
    @MainThread
    void onLogin(@NonNull ZapicPlayer player);

    /**
     * Invoked after the player has logged out.
     * <p>
     * This passes the previous player as a convenience. The game may use this callback to disable
     * features reserved for authenticated players. The game may also use this callback to reset
     * any game data.
     *
     * @param player The previous player.
     */
    @MainThread
    void onLogout(@NonNull ZapicPlayer player);
}
