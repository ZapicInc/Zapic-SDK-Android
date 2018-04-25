package com.zapic.sdk.android;

import android.app.Activity;
import android.app.Application;
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
 * It is the responsibility of the game's {@link Application} to call the
 * {@link #start(AuthenticationHandler)} method during its {@code onCreate} lifecycle method. The
 * game may optionally register an authentication handler that is notified when a player is logged
 * in or out.
 * <p>
 * It is the responsibility of the game's activity (or game's activities if there are multiple) to
 * call the {@link #attachFragment(Activity)} method during its {@code onCreate} lifecycle method.
 * This method creates and attaches a non-UI fragment, {@link ZapicFragment}, to the activity. The
 * {@link ZapicFragment} downloads and runs the Zapic JavaScript application in the background. If a
 * game's activity does <i>not</i> call {@link #attachFragment(Activity)}, the Zapic JavaScript
 * application may be garbage collected and the current player's session reset. It is also the
 * responsibility of the game's activity to call the {@link #show(Activity)} method after the player
 * interacts with a Zapic-branded button visible from the game's main menu (requirements are
 * outlined in the <a href="https://www.zapic.com/terms/">Terms of Use</a>).
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
     * The notification tag containing the Zapic referral content.
     */
    @NonNull
    @SuppressWarnings("unused")
    public static final String NOTIFICATION_TAG = "zapic_player_token";

    /**
     * The authentication handler.
     */
    @Nullable
    private static volatile Zapic.AuthenticationHandler authenticationHandler;

    /**
     * The current player.
     */
    @Nullable
    private static volatile ZapicPlayer player;

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
            manager.beginTransaction().add(new ZapicFragment(), Zapic.FRAGMENT_TAG).commit();
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
     * Gets the current player.
     *
     * @return The current player or {@code null} if the current player has not logged in.
     */
    @AnyThread
    @CheckResult
    @Nullable
    @SuppressWarnings({"UnusedDeclaration", "WeakerAccess"}) // documented as public API
    public static ZapicPlayer getPlayer() {
        return Zapic.player;
    }

    /**
     * Handles custom data provided by a push notification, deep link, etc..
     *
     * @param gameActivity The game's activity.
     * @param data         The JSON-encoded object of gameplay event parameters.
     * @throws IllegalArgumentException If {@code gameActivity} or {@code data} are {@code null}; if
     *                                  {@code gameActivity} does not have a {@link ZapicFragment}
     *                                  attached (see {@link #attachFragment(Activity)}; if
     *                                  {@code data} is not a valid JSON object.
     */
    @AnyThread
    public static void handleData(@Nullable final Activity gameActivity, @Nullable final JSONObject data) {
        if (gameActivity == null) {
            throw new IllegalArgumentException("gameActivity must not be null");
        }

        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }

        try {
            String value = data.getString("zapic");
            WebViewManager.getInstance().handleData(value);
        } catch (JSONException ignored) {
        }
    }

    /**
     * Handles custom data provided by a push notification, deep link, etc..
     *
     * @param gameApplication The game's application.
     * @param data            The JSON-encoded object of gameplay event parameters.
     * @throws IllegalArgumentException If {@code gameApplication} or {@code data} are {@code null};
     *                                  if {@code data} is not a valid JSON object.
     */
    @AnyThread
    public static void handleData(@Nullable final Application gameApplication, @Nullable final JSONObject data) {
        if (gameApplication == null) {
            throw new IllegalArgumentException("gameApplication must not be null");
        }

        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }

        try {
            String value = data.getString("zapic");
            WebViewManager.getInstance().handleData(value);
        } catch (JSONException ignored) {
        }
    }

    /**
     * Sets the current player.
     *
     * @param player The current player or {@code null} if the current player has logged out.
     */
    @AnyThread
    static void setPlayer(@Nullable final ZapicPlayer player) {
        final ZapicPlayer previousPlayer = Zapic.player;
        Zapic.player = player;

        final Zapic.AuthenticationHandler authenticationHandler = Zapic.authenticationHandler;
        if (authenticationHandler != null) {
            if (previousPlayer != null) {
                authenticationHandler.onLogout(previousPlayer);
            }

            if (player != null) {
                authenticationHandler.onLogin(player);
            }
        }
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
     * Initializes Zapic and, optionally, registers an authentication handler that is notified when
     * a player is logged in or out.
     * <p>
     * This must only be called once for the lifetime of the application.
     *
     * @param authenticationHandler The authentication handler.
     * @throws IllegalArgumentException If {@code application} is {@code null}.
     * @since 1.0.2
     */
    @AnyThread
    @SuppressWarnings("unused")
    public static void start(@Nullable Zapic.AuthenticationHandler authenticationHandler) {
        Zapic.authenticationHandler = authenticationHandler;
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
    @AnyThread
    @SuppressWarnings({"UnusedDeclaration", "WeakerAccess"}) // documented as public API
    public static void submitEvent(@Nullable final Activity gameActivity, @Nullable final String parameters) {
        if (gameActivity == null) {
            throw new IllegalArgumentException("gameActivity must not be null");
        }

        if (parameters == null) {
            throw new IllegalArgumentException("parameters must not be null");
        }

        JSONObject params;
        try {
            params = new JSONObject(parameters);
        } catch (JSONException e) {
            throw new IllegalArgumentException("parameters must be a valid JSON object");
        }

        JSONObject gameplayEvent;
        try {
            gameplayEvent = new JSONObject().put("type", "gameplay").put("params", params);
        } catch (JSONException ignored) {
            return;
        }

        WebViewManager.getInstance().submitEvent(gameplayEvent);
    }

    /**
     * Represents an authentication handler that is notified when a player is logged in or out.
     *
     * @author Kyle Dodson
     * @since 1.0.2
     */
    public interface AuthenticationHandler {
        /**
         * The current player.
         *
         * @param player The current player.
         */
        @AnyThread
        void onLogin(@NonNull ZapicPlayer player);

        /**
         * The previous player.
         *
         * @param player The previous player.
         */
        @AnyThread
        void onLogout(@NonNull ZapicPlayer player);
    }
}
