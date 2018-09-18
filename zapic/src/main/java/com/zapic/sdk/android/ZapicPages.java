package com.zapic.sdk.android;

import android.app.Activity;
import android.support.annotation.NonNull;

/**
 * Provides constant values that identify the different pages that may be shown using
 * {@link Zapic#showPage(Activity, String)}.
 *
 * @author Kyle Dodson
 * @since 1.2.0
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class ZapicPages {
    /**
     * Identifies a page that shows the list of challenges for the current game.
     */
    @NonNull
    public static final String CHALLENGE_LIST = "Challenges";

    /**
     * Identifies a page that shows the list of competitions for the current game.
     */
    @NonNull
    public static final String COMPETITION_LIST = "Competitions";

    /**
     * Identifies a page that shows the create a new challenge form for the current game.
     */
    @NonNull
    public static final String CREATE_CHALLENGE = "CreateChallenge";

    /**
     * Identifies a page that shows the login form for the current player.
     */
    @NonNull
    public static final String LOGIN = "Login";

    /**
     * Identifies a page that shows the profile for the current player.
     */
    @NonNull
    public static final String PROFILE = "Profile";

    /**
     * Identifies a page that shows the list of statistics for the current player in the current
     * game.
     */
    @NonNull
    public static final String STAT_LIST = "Stats";
}
