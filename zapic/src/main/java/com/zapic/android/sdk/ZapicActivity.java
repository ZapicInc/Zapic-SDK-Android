package com.zapic.android.sdk;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.transition.Slide;
import android.util.Log;
import android.webkit.WebView;

/**
 * An {@link Activity} that presents the Zapic JavaScript application in the foreground.
 * <p>
 * Use {@link Zapic#show(Activity)} or {@link Zapic#show(Activity, String)} to start instances of
 * this activity. Alternatively, use the {@link #createIntent(Activity)} or
 * {@link #createIntent(Activity, String)} factory methods to create an intent that starts instances
 * of this activity.
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
public final class ZapicActivity extends Activity implements
        LoadingPageFragment.InteractionListener,
        OfflinePageFragment.InteractionListener {
    /**
     * The {@link Intent} parameter that identifies the Zapic JavaScript application page to open.
     */
    @NonNull
    private static final String PAGE_PARAM = "page";

    /**
     * The tag used to identify log messages.
     */
    @NonNull
    private static final String TAG = "ZapicActivity";

    /**
     * The Zapic JavaScript application.
     */
    @Nullable
    private App mApp;

    /**
     * Creates a new {@link ZapicActivity} instance.
     */
    @MainThread
    public ZapicActivity() {
        this.mApp = null;
    }

    /**
     * Creates an {@link Intent} that starts a {@link ZapicActivity} and opens the default Zapic
     * JavaScript application page.
     *
     * @param gameActivity The game's activity.
     * @return             The {@link Intent}.
     */
    @MainThread
    @CheckResult
    @NonNull
    public static Intent createIntent(@NonNull final Activity gameActivity) {
        return ZapicActivity.createIntent(gameActivity, "default");
    }

    /**
     * Creates an {@link Intent} that starts a {@link ZapicActivity} and opens the specified Zapic
     * JavaScript application page.
     *
     * @param gameActivity The game's activity.
     * @param page         The Zapic JavaScript application page to open.
     * @return             The {@link Intent}.
     */
    @MainThread
    @CheckResult
    @NonNull
    public static Intent createIntent(@NonNull final Activity gameActivity, @NonNull final String page) {
        final Intent intent = new Intent(gameActivity, ZapicActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(ZapicActivity.PAGE_PARAM, page);
        return intent;
    }

    @Override
    public void close() {
        this.finish();
    }

// TODO: Determine if the following is required when we use a fullscreen theme.
//    /**
//     * Enables an immersive full-screen mode. This hides the system status and navigation bars until
//     * the user swipes in from the edges of the screen.
//     */
//    @MainThread
//    private void enableImmersiveFullScreenMode() {
//        this.getWindow().getDecorView().setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_FULLSCREEN
//                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                        | View.SYSTEM_UI_FLAG_LOW_PROFILE);
//    }

    /**
     * Gets the Zapic JavaScript application.
     */
    @CheckResult
    @MainThread
    @Nullable
    App getApp() {
        return this.mApp;
    }

    @MainThread
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreate");
        }

        super.onCreate(savedInstanceState);

// TODO: Determine if the following is required when we use a fullscreen theme.
//        // Hide the system status and navigation bars.
//        this.enableImmersiveFullScreenMode();

        this.setContentView(R.layout.activity_zapic);
        this.openLoadingPage();

        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final Slide slide = new Slide();
            slide.setDuration(500);
            this.getWindow().setEnterTransition(slide);
        }

        // Attach Zapic.
        Zapic.attachFragment(this);
    }

    @MainThread
    @Override
    protected void onNewIntent(final Intent intent) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onNewIntent");
        }

        super.onNewIntent(intent);

        this.setIntent(intent);
        if (this.mApp != null) {
            // TODO: Navigate to the requested page.
        }
    }

// TODO: Determine if the following is required when we use a fullscreen theme.
//    @MainThread
//    @Override
//    public void onWindowFocusChanged(final boolean hasFocus) {
//        if (BuildConfig.DEBUG) {
//            Log.d(TAG, "onWindowFocusChanged");
//        }
//
//        super.onWindowFocusChanged(hasFocus);
//
//        // Hide the system status and navigation bars.
//        if (hasFocus) {
//            this.enableImmersiveFullScreenMode();
//        }
//    }

    /**
     * Opens the Zapic JavaScript application page.
     */
    @MainThread
    void openAppPage() {
        final FragmentManager fragmentManager = this.getFragmentManager();
        final Fragment currentFragment = fragmentManager.findFragmentById(R.id.activity_zapic_container);
        if (currentFragment == null) {
            fragmentManager.beginTransaction().add(R.id.activity_zapic_container, AppPageFragment.createInstance()).commit();
        } else if (!(currentFragment instanceof AppPageFragment)) {
            fragmentManager.beginTransaction().replace(R.id.activity_zapic_container, AppPageFragment.createInstance()).commit();
        }
    }

    /**
     * Opens the loading page.
     */
    @MainThread
    void openLoadingPage() {
        final FragmentManager fragmentManager = this.getFragmentManager();
        final Fragment currentFragment = fragmentManager.findFragmentById(R.id.activity_zapic_container);
        if (currentFragment == null) {
            fragmentManager.beginTransaction().add(R.id.activity_zapic_container, LoadingPageFragment.createInstance()).commit();
        } else if (!(currentFragment instanceof LoadingPageFragment)) {
            fragmentManager.beginTransaction().replace(R.id.activity_zapic_container, LoadingPageFragment.createInstance()).commit();
        }
    }

    /**
     * Opens the offline page.
     */
    @MainThread
    void openOfflinePage() {
        final FragmentManager fragmentManager = this.getFragmentManager();
        final Fragment currentFragment = fragmentManager.findFragmentById(R.id.activity_zapic_container);
        if (currentFragment == null) {
            fragmentManager.beginTransaction().add(R.id.activity_zapic_container, OfflinePageFragment.createInstance()).commit();
        } else if (!(currentFragment instanceof OfflinePageFragment)) {
            fragmentManager.beginTransaction().replace(R.id.activity_zapic_container, OfflinePageFragment.createInstance()).commit();
        }
    }

    /**
     * Sets the Zapic JavaScript application.
     *
     * @param app The Zapic JavaScript application.
     */
    @MainThread
    void setApp(@Nullable final App app) {
        this.mApp = app;
    }

    //region Lifecycle Events

    // onCreate

    @MainThread
    @Override
    protected void onStart() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onStart");
        }

        super.onStart();
    }

    @MainThread
    @Override
    protected void onResume() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onResume");
        }

        super.onResume();
    }

    @MainThread
    @Override
    protected void onPause() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onPause");
        }

        super.onPause();
    }

    @MainThread
    @Override
    protected void onStop() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onStop");
        }

        super.onStop();
    }

    @MainThread
    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onDestroy");
        }

        super.onDestroy();
    }

    //endregion
}
