package com.zapic.android.sdk;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.support.annotation.CheckResult;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Provides static methods to manage the Zapic application.
 * <p>
 * The Zapic application runs in a {@link WebView}. Generally, the {@link WebView} runs for the
 * lifetime of the Android application. While the game is in focus, the {@link WebView} runs in
 * the background (managed by a {@link ZapicFragment} attached to the game's activity) and the Zapic
 * application processes events and receives notifications. When the player shifts focus to the
 * Zapic application, the {@link WebView} moves to the foreground and is presented by a
 * {@link ZapicActivity}. When the player shifts focus back to the game, the {@link ZapicActivity}
 * finishes and the {@link WebView} again runs in the background (again managed by a
 * {@link ZapicFragment} attached to the game's activity).
 * <p>
 * It is the responsibility of the game's activity (or activities if there are multiple) to call
 * {@link Zapic#attachFragment(Activity)} during its {@code onCreate} lifecycle callback. This will
 * create and attach a {@link ZapicFragment} to the game's activity. If the game's activity does
 * <i>not</i> call {@link Zapic#attachFragment(Activity)}, the {@link WebView} may be garbage
 * collected by the system.
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
    private static String playerId;

    /**
     * Prevents creating a new instance.
     */
    private Zapic() {
    }

    /**
     * Creates and attaches a {@link ZapicFragment} to the specified game's activity.
     * <p>
     * This must be called from the Android application's main thread.
     *
     * @param  gameActivity             The game's activity.
     * @throws IllegalArgumentException If {@code gameActivity} is {@code null}.
     */
    @MainThread
    @SuppressWarnings("UnusedDeclaration") // documented as public API
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
     * called. The fragment is automatically destroyed and garbage collected when the game's
     * activity finishes.
     *
     * @param  gameActivity             The game's activity.
     * @throws IllegalArgumentException If {@code gameActivity} is {@code null}.
     */
    @MainThread
    @SuppressWarnings("UnusedDeclaration") // documented as public API
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
     * Gets the player's unique identifier.
     *
     * @return The player's unique identifier or {@code null} if the player has not logged in.
     */
    @CheckResult
    @Nullable
    @SuppressWarnings("UnusedDeclaration") // documented as public API
    public static String getPlayerId() {
        return Zapic.playerId;
    }

    /**
     * Sets the player's unique identifier.
     *
     * @param playerId The player's unique identifier or {@code null} if the player has logged out.
     */
    static void setPlayerId(@Nullable final String playerId) {
        Zapic.playerId = playerId;
    }

    /**
     * Shows the Zapic application.
     * <p>
     * This must be called from the Android application's main thread. This starts a
     * {@link ZapicActivity} and opens the default Zapic application page.
     * <p>
     * This <i>must</i> be called from the Zapic-branded button visible from the game's main menu
     * (requirements are outlined in the <a href="https://www.zapic.com/terms/">Terms of Use</a>).
     * This <i>may</i> be called from other game elements or events.
     *
     * @param gameActivity              The game's activity.
     * @throws IllegalArgumentException If {@code gameActivity} is {@code null}.
     */
    @MainThread
    @SuppressWarnings("UnusedDeclaration") // documented as public API
    public static void show(@Nullable final Activity gameActivity) {
        if (gameActivity == null) {
            throw new IllegalArgumentException("gameActivity must not be null");
        }

        final Intent intent = ZapicActivity.createIntent(gameActivity);
        gameActivity.startActivity(intent);
    }

    /**
     * Shows the Zapic application and opens the specified Zapic application page.
     * <p>
     * This must be called from the Android application's main thread. This starts a
     * {@link ZapicActivity} and opens the default Zapic application page.
     * <p>
     * This <i>must not</i> be called from the Zapic-branded button visible from the game's main
     * menu (see {@link Zapic#show(Activity)}. This <i>may</i> be called from other game elements or
     * events to refer players to more specific pages.
     *
     * @param gameActivity              The game's activity.
     * @param page                      The Zapic application page to open.
     * @throws IllegalArgumentException If {@code gameActivity} or {@code page} are {@code null}.
     */
    @MainThread
    @SuppressWarnings("UnusedDeclaration") // documented as public API
    public static void show(@Nullable final Activity gameActivity, @Nullable final String page) {
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
     * @param gameActivity              The game's activity.
     * @param parameters                The JSON-encoded object of gameplay event parameters.
     * @throws IllegalArgumentException If {@code gameActivity} or {@code parameters} are
     *                                  {@code null} or if {@code parameters} is not a valid JSON
     *                                  object.
     */
    @MainThread
    @SuppressWarnings("UnusedDeclaration") // documented as public API
    public static void submitEvent(@Nullable Activity gameActivity, @Nullable String parameters) {
        if (gameActivity == null) {
            throw new IllegalArgumentException("gameActivity must not be null");
        }

        if (parameters == null) {
            throw new IllegalArgumentException("parameters must not be null");
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
        } catch (JSONException e) {
            throw new IllegalArgumentException("payload must be a valid JSON object");
        }

        // TODO: Submit event.
        Log.d("Zapic", String.format("Gameplay Event: %s", gameplayEvent));
    }
}
