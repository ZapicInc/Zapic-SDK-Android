package com.zapic.android.sdk;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.support.annotation.AnyThread;
import android.support.annotation.CheckResult;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Provides static methods to manage and interact with Zapic.
 * <p>
 * It is the responsibility of the game's activity (or game's activities if there are multiple) to
 * call the {@link #attachFragment(Activity)} method during its {@code onCreate} lifecycle method.
 * This method creates and attaches a non-UI fragment, {@link ZapicFragment}, to the activity. The
 * {@link ZapicFragment} downloads and runs the Zapic JavaScript application. If a game's activity
 * does <i>not</i> call {@link #attachFragment(Activity)}, the Zapic JavaScript application may be
 * garbage collected and the current player's session reset. It is also the responsibility of the
 * game's activity to call the {@link #show(Activity)} method after the player interacts with a
 * Zapic-branded button visible from the game's main menu (requirements are outlined in the
 * <a href="https://www.zapic.com/terms/">Terms of Use</a>).
 * <p>
 * The Zapic JavaScript application runs in a {@link WebView}. Generally, the {@link WebView} runs
 * for the lifetime of the Android application. While the game is in focus, the {@link WebView} runs
 * in the background (managed by one or more {@link ZapicFragment}s attached to the game's
 * activities). The Zapic JavaScript application processes events and receives notifications while
 * running in the background. When the player shifts focus to Zapic, the {@link WebView} moves to
 * the foreground and is presented by a {@link ZapicActivity}. When the player shifts focus back to
 * the game, the {@link ZapicActivity} finishes and the {@link WebView} returns to the background
 * (again managed by one or more {@link ZapicFragment}s attached to the game's activities).
 *
 * @author Kyle Dodson
 * @since 1.0.0
 */
public final class Zapic {
    /**
     * The fragment tag used to identify {@link ZapicFragment} instances.
     */
    @NonNull
    private static final String FRAGMENT_TAG = "Zapic";

    /**
     * The player's unique identifier.
     */
    @Nullable
    private static volatile String playerId;

    /**
     * Prevents creating a new {@link Zapic} instance.
     */
    private Zapic() {
    }

    /**
     * Creates and attaches a {@link ZapicFragment} to the specified game's activity.
     * <p>
     * This must be called from the Android application's main thread.
     *
     * @param gameActivity The game's activity.
     * @throws IllegalArgumentException If {@code gameActivity} is {@code null}.
     */
    @MainThread
    @SuppressWarnings({"UnusedDeclaration", "WeakerAccess"}) // documented as public API
    public static void attachFragment(@Nullable final Activity gameActivity) {
        if (gameActivity == null) {
            throw new IllegalArgumentException("gameActivity must not be null");
        }

        final FragmentManager manager = gameActivity.getFragmentManager();
        final Fragment fragment = manager.findFragmentByTag(Zapic.FRAGMENT_TAG);
        if (fragment == null) {
            manager.beginTransaction().add(ZapicFragment.createInstance(), Zapic.FRAGMENT_TAG).commit();
        }
    }

    /**
     * Detaches a {@link ZapicFragment} from the specified game's activity.
     * <p>
     * This must be called from the Android application's main thread. Generally, this should not be
     * called. The fragment is automatically destroyed when the game's activity finishes.
     *
     * @param gameActivity The game's activity.
     * @throws IllegalArgumentException If {@code gameActivity} is {@code null}.
     */
    @MainThread
    @SuppressWarnings({"UnusedDeclaration", "WeakerAccess"}) // documented as public API
    public static void detachFragment(@Nullable final Activity gameActivity) {
        if (gameActivity == null) {
            throw new IllegalArgumentException("gameActivity must not be null");
        }

        final FragmentManager manager = gameActivity.getFragmentManager();
        final Fragment fragment = manager.findFragmentByTag(Zapic.FRAGMENT_TAG);
        if (fragment != null) {
            manager.beginTransaction().remove(fragment).commit();
        }
    }

    /**
     * Gets a {@link ZapicFragment} from the specified game's activity.
     *
     * @param gameActivity The game's activity.
     * @return The {@link ZapicFragment} or {@code null} if one has not been attached.
     */
    @CheckResult
    @MainThread
    @Nullable
    static ZapicFragment getFragment(@NonNull final Activity gameActivity) {
        final FragmentManager manager = gameActivity.getFragmentManager();
        final Fragment fragment = manager.findFragmentByTag(Zapic.FRAGMENT_TAG);
        return fragment instanceof ZapicFragment ? (ZapicFragment) fragment : null;
    }

    /**
     * Gets the player's unique identifier.
     *
     * @return The player's unique identifier or {@code null} if the player has not logged in.
     */
    @AnyThread
    @CheckResult
    @Nullable
    @SuppressWarnings({"UnusedDeclaration", "WeakerAccess"}) // documented as public API
    public static String getPlayerId() {
        return Zapic.playerId;
    }

    /**
     * Sets the player's unique identifier.
     *
     * @param playerId The player's unique identifier or {@code null} if the player has logged out.
     */
    @AnyThread
    static void setPlayerId(@Nullable final String playerId) {
        Zapic.playerId = playerId;
    }

    /**
     * Shows the Zapic JavaScript application and opens the default page.
     * <p>
     * This must be called from the Android application's main thread. This starts a
     * {@link ZapicActivity} and opens the default Zapic JavaScript application page.
     * <p>
     * This <i>must</i> be called from the Zapic-branded button visible from the game's main menu
     * (requirements are outlined in the <a href="https://www.zapic.com/terms/">Terms of Use</a>).
     * This <i>may</i> be called from other game elements or user interactions.
     *
     * @param gameActivity The game's activity.
     * @throws IllegalArgumentException If {@code gameActivity} is {@code null}.
     */
    @MainThread
    @SuppressWarnings({"UnusedDeclaration", "WeakerAccess"}) // documented as public API
    public static void show(@Nullable final Activity gameActivity) {
        if (gameActivity == null) {
            throw new IllegalArgumentException("gameActivity must not be null");
        }

        final Intent intent = ZapicActivity.createIntent(gameActivity);
        gameActivity.startActivity(intent);
    }

    /**
     * Shows the Zapic JavaScript application and opens the specified page.
     * <p>
     * This must be called from the Android application's main thread. This starts a
     * {@link ZapicActivity} and opens the specified Zapic JavaScript application page.
     * <p>
     * This <i>must not</i> be called from the Zapic-branded button visible from the game's main
     * menu (see {@link #show(Activity)}. This <i>may</i> be called from other game elements or
     * user interactions to navigate players to specific pages.
     *
     * @param gameActivity The game's activity.
     * @param page         The Zapic JavaScript application page to open.
     * @throws IllegalArgumentException If {@code gameActivity} or {@code page} are {@code null}.
     */
    @MainThread
    @SuppressWarnings({"UnusedDeclaration", "WeakerAccess"}) // documented as public API
    public static void show(@Nullable final Activity gameActivity, @SuppressWarnings("SameParameterValue") @Nullable final String page) {
        if (gameActivity == null) {
            throw new IllegalArgumentException("gameActivity must not be null");
        }

        if (page == null) {
            throw new IllegalArgumentException("page must not be null");
        }

        final Intent intent = ZapicActivity.createIntent(gameActivity, page);
        gameActivity.startActivity(intent);
    }

    /**
     * Submits a gameplay event to Zapic.
     *
     * @param gameActivity The game's activity.
     * @param parameters   The JSON-encoded object of gameplay event parameters.
     * @throws IllegalArgumentException If {@code gameActivity} or {@code parameters} are
     *                                  {@code null}; if {@code gameActivity} does not have a
     *                                  {@link ZapicFragment} attached (see
     *                                  {@link #attachFragment(Activity)}; if {@code parameters} is
     *                                  not a valid JSON object.
     */
    @MainThread
    @SuppressWarnings({"UnusedDeclaration", "WeakerAccess"}) // documented as public API
    public static void submitEvent(@Nullable final Activity gameActivity, @Nullable final String parameters) {
        if (gameActivity == null) {
            throw new IllegalArgumentException("gameActivity must not be null");
        }

        if (parameters == null) {
            throw new IllegalArgumentException("parameters must not be null");
        }

        final ZapicFragment fragment = Zapic.getFragment(gameActivity);
        if (fragment == null) {
            throw new IllegalArgumentException("gameActivity must have a ZapicFragment attached");
        }

        final App app = fragment.getApp();
        if (app == null) {
            throw new IllegalArgumentException("gameActivity must have a ZapicFragment attached");
        }

        JSONObject payload;
        try {
            payload = new JSONObject(parameters);
        } catch (JSONException e) {
            throw new IllegalArgumentException("payload must be a valid JSON object");
        }

        JSONObject gameplayEvent;
        try {
            gameplayEvent = new JSONObject().put("type", "gameplay").put("payload", payload);
        } catch (JSONException ignored) {
            return;
        }

        app.submitEvent(gameplayEvent);
    }
}
