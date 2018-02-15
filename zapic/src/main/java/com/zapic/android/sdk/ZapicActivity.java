package com.zapic.android.sdk;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

/**
 * An {@link Activity} that presents the Zapic application.
 * <p>
 * Use the {@link ZapicActivity#createIntent} factory method to create an intent that starts this
 * activity and opens a specific Zapic application page.
 * <p>
 * The Zapic application runs in a {@link WebView}. Generally, the {@link WebView} runs for the
 * lifetime of the Android application. While the game is in focus, the {@link WebView} runs in
 * the background (managed by a {@link ZapicFragment} attached to the game's activity) and the Zapic
 * application processes events and receives notifications. When the player shifts focus to the
 * Zapic application, the {@link WebView} moves to the foreground and is presented by a
 * {@link ZapicActivity}. When the player shifts focus back to the game, the {@link ZapicActivity}
 * finishes and the {@link WebView} again runs in the background (again managed by a
 * {@link ZapicFragment} attached to the game's activity).
 *
 * @author Kyle Dodson
 * @since 1.0.0
 */
public final class ZapicActivity extends Activity implements
        AppManager.StateChangedListener,
        LoadingPageFragment.InteractionListener,
        OfflinePageFragment.InteractionListener {
    /**
     * The fragment tag used to identify {@link ZapicFragment} instances.
     */
    @NonNull
    private static final String FRAGMENT_TAG = "Zapic";

    /**
     * The {@link Intent} parameter that identifies the Zapic application page to open.
     */
    @NonNull
    private static final String PAGE_PARAM = "page";

    /**
     * The tag used to identify log messages.
     */
    @NonNull
    private static final String TAG = "ZapicFragment";

    /**
     * The Zapic application manager.
     */
    @Nullable
    private AppManager mAppManager;

    /**
     * Creates a new instance.
     */
    public ZapicActivity() {
        this.mAppManager = null;
    }

    /**
     * Creates an {@link Intent} that starts a {@link ZapicActivity} and opens the default Zapic
     * application page.
     *
     * @param gameActivity The game's activity.
     * @return             The {@link Intent}.
     */
    @CheckResult
    @NonNull
    public static Intent createIntent(@NonNull final Activity gameActivity) {
        return ZapicActivity.createIntent(gameActivity, "default");
    }

    /**
     * Creates an {@link Intent} that starts a {@link ZapicActivity} and opens the specified Zapic
     * application page.
     *
     * @param gameActivity The game's activity.
     * @param page         The Zapic application page to open.
     * @return             The {@link Intent}.
     */
    @CheckResult
    @NonNull
    public static Intent createIntent(@NonNull final Activity gameActivity, @NonNull final String page) {
        final Intent intent = new Intent(gameActivity, ZapicActivity.class);
        intent.putExtra(ZapicActivity.PAGE_PARAM, page);
        return intent;
    }

    /**
     * Enables an immersive full-screen mode. This hides the system status and navigation bars until
     * the user swipes in from the edges of the screen.
     */
    @MainThread
    private void enableImmersiveFullScreenMode() {
        this.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LOW_PROFILE);
    }

    @MainThread
    @Override
    public void onAppStarted() {
        assert this.mAppManager != null : "mAppManager == null";

        // TODO: Show loading page, dispatch OPEN_PAGE, wait for PAGE_READY.
        final FragmentManager fragmentManager = this.getFragmentManager();
        final Fragment currentFragment = fragmentManager.findFragmentById(R.id.activity_zapic_container);
        if (!(currentFragment instanceof AppPageFragment)) {
            fragmentManager.beginTransaction().add(R.id.activity_zapic_container, AppPageFragment.createInstance()).commit();
        }
    }

    @Override
    public void onOffline() {
        assert this.mAppManager != null : "mAppManager == null";
        if (this.mAppManager.isAppStarted()) {
            return;
        }

        final FragmentManager fragmentManager = this.getFragmentManager();
        final Fragment currentFragment = fragmentManager.findFragmentById(R.id.activity_zapic_container);
        if (!(currentFragment instanceof OfflinePageFragment)) {
            fragmentManager.beginTransaction().add(R.id.activity_zapic_container, OfflinePageFragment.createInstance()).commit();
        }
    }

    @Override
    public void onOnline() {
        assert this.mAppManager != null : "mAppManager == null";
        if (this.mAppManager.isAppStarted()) {
            return;
        }

        final FragmentManager fragmentManager = this.getFragmentManager();
        final Fragment currentFragment = fragmentManager.findFragmentById(R.id.activity_zapic_container);
        if (!(currentFragment instanceof LoadingPageFragment)) {
            fragmentManager.beginTransaction().add(R.id.activity_zapic_container, LoadingPageFragment.createInstance()).commit();
        }
    }

    @MainThread
    @Override
    public void onClose() {
        this.finish();
    }

    @MainThread
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        // Hide the system status and navigation bars.
        this.enableImmersiveFullScreenMode();

        // Get a reference to the AppManager (to keep it from being garbage collected).
        if (this.mAppManager == null) {
            this.mAppManager = AppManager.getInstance();
        }

        // Listen to application state changes.
        this.mAppManager.addStateChangedListener(this);

        // Render the page.
        this.setContentView(R.layout.activity_zapic);
        final Fragment fragment;
        if (this.mAppManager.isAppStarted()) {
            // TODO: Show loading page, dispatch OPEN_PAGE, wait for PAGE_READY.
            fragment = AppPageFragment.createInstance();
        } else if (this.mAppManager.isOffline()) {
            fragment = OfflinePageFragment.createInstance();
        } else {
            fragment = LoadingPageFragment.createInstance();
        }

        final FragmentManager fragmentManager = this.getFragmentManager();
        fragmentManager.beginTransaction().add(R.id.activity_zapic_container, fragment).commit();
        fragmentManager.beginTransaction().add(ZapicFragment.createInstance(), ZapicActivity.FRAGMENT_TAG).commit();
    }

    @MainThread
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();

        // Destroy the WebView (if this is the last view).
        assert this.mAppManager != null : "mAppManager == null";
        this.mAppManager.removeStateChangedListener(this);
        this.mAppManager = null;
    }

    @MainThread
    @Override
    protected void onNewIntent(final Intent intent) {
        Log.d(TAG, "onNewIntent");
        super.onNewIntent(intent);
        this.setIntent(intent);

        // TODO: If the app is loaded, navigate to the requested page. If the app is not loaded,
        // ensure we navigate to the newly requested page.
    }

    @MainThread
    @Override
    public void onWindowFocusChanged(final boolean hasFocus) {
        Log.d(TAG, "onWindowFocusChanged");
        super.onWindowFocusChanged(hasFocus);

        // Hide the system status and navigation bars.
        if (hasFocus) {
            this.enableImmersiveFullScreenMode();
        }
    }

    //region Lifecycle Events

    // onCreate

    @MainThread
    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
    }

    @MainThread
    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
    }

    @MainThread
    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @MainThread
    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    // onDestroy

    //endregion
}
