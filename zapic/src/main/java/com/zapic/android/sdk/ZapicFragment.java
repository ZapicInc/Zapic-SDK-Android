package com.zapic.android.sdk;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import java.lang.ref.WeakReference;

/**
 * A {@link Fragment} that runs the Zapic JavaScript application in the background.
 * <p>
 * Use {@link Zapic#attachFragment(Activity)} to create and attach instances of this fragment to
 * your game's activity. Alternatively, use the {@link #createInstance()} factory method to create
 * instances of this fragment.
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
public final class ZapicFragment extends Fragment {
    /**
     * The Zapic JavaScript application manager. This starts and stops the Zapic JavaScript
     * application when the first {@link ZapicFragment} is created and the last
     * {@link ZapicFragment} is destroyed, respectively.
     * <p>
     * Use the {@link #getInstance()} method to get the singleton instance.
     *
     * @author Kyle Dodson
     * @since 1.0.0
     * @see App
     * @see ZapicFragment
     */
    private static final class AppManager {
        /**
         * A weak reference to the {@link AppManager} "singleton instance". Technically, as a weak
         * reference, this is not a singleton instance as defined by the software design pattern.
         * The weak reference is required, though. It ensures we don't leak the Android
         * application's context. In practice, we cannot discern this implementation difference.
         * {@link ZapicFragment} instances keep a strong reference to this {@link AppManager}
         * instance. As such, this {@link AppManager} instance can only be garbage collected after
         * all of our {@link ZapicFragment} instances have been garbage collected. This is no
         * different from the state at initial launch.
         */
        @NonNull
        private static WeakReference<AppManager> INSTANCE = new WeakReference<>(null);

        /**
         * Gets the {@link AppManager} singleton instance.
         *
         * @return The {@link AppManager} singleton instance.
         */
        @MainThread
        @NonNull
        static AppManager getInstance() {
            AppManager instance = AppManager.INSTANCE.get();
            if (instance == null) {
                instance = new AppManager();
                AppManager.INSTANCE = new WeakReference<>(instance);
            }

            return instance;
        }

        /**
         * The Zapic JavaScript application.
         */
        @NonNull
        private final App mApp;

        /**
         * The number of {@link ZapicFragment} instances that have been created.
         */
        private int mFragmentCount;

        /**
         * Creates a new {@link AppManager} instance.
         */
        @MainThread
        private AppManager() {
            this.mApp = new App();
            this.mFragmentCount = 0;
        }

        /**
         * Adds the {@link WebView} to the {@link ZapicActivity}.
         *
         * @param activity The activity to which the fragment was attached.
         */
        @MainThread
        void onAttached(@NonNull final Activity activity) {
            if (activity instanceof ZapicActivity) {
                final ZapicActivity zapicActivity = (ZapicActivity)activity;
                zapicActivity.setApp(this.mApp);
                this.mApp.setActivity(zapicActivity);
            }
        }

        /**
         * Increments the number of {@link ZapicFragment} instances that have been created and, if
         * this is the first fragment, starts the Zapic JavaScript application.
         *
         * @param activity The activity to which the fragment was attached.
         */
        @MainThread
        void onCreated(@NonNull final Activity activity) {
            ++this.mFragmentCount;

            if (activity instanceof ZapicActivity) {
                final ZapicActivity zapicActivity = (ZapicActivity)activity;
                zapicActivity.setApp(this.mApp);
                this.mApp.setActivity(zapicActivity);
            }

            // Start the application if this is the first fragment.
            if (this.mFragmentCount == 1) {
                this.mApp.start(activity);
            }
        }

        /**
         * Decrements the number of {@link ZapicFragment} instances that have been created and, if
         * this is the last fragment, stops the Zapic JavaScript application.
         *
         * @param activity The activity to which the fragment was attached.
         */
        @MainThread
        void onDestroyed(@NonNull final Activity activity) {
            --this.mFragmentCount;

            if (activity instanceof ZapicActivity) {
                final ZapicActivity zapicActivity = (ZapicActivity)activity;
                zapicActivity.setApp(null);
                this.mApp.setActivity(null);
            }

            // Stop the application if this is the last fragment.
            if (this.mFragmentCount == 0) {
                this.mApp.stop();
            }
        }

        /**
         * Removes the {@link WebView} from the {@link ZapicActivity}.
         *
         * @param activity The activity to which the fragment was attached.
         */
        @MainThread
        void onDetached(@NonNull final Activity activity) {
            if (activity instanceof ZapicActivity) {
                final ZapicActivity zapicActivity = (ZapicActivity)activity;
                zapicActivity.setApp(null);
                this.mApp.setActivity(null);
            }
        }
    }

    /**
     * The tag used to identify log messages.
     */
    @NonNull
    private static final String TAG = "ZapicFragment";

    /**
     * The application manager.
     */
    @Nullable
    private AppManager mAppManager;

    /**
     * The broadcast receiver that relays network connectivity changes.
     */
    @Nullable
    private ConnectivityBroadcastReceiver mConnectivityBroadcastReceiver;

    /**
     * Creates a new {@link ZapicFragment} instance.
     */
    @MainThread
    public ZapicFragment() {
        this.mAppManager = null;
        this.mConnectivityBroadcastReceiver = null;
    }

    /**
     * Creates a new {@link ZapicFragment} instance.
     *
     * @return The new {@link ZapicFragment} instance.
     */
    @CheckResult
    @MainThread
    @NonNull
    public static ZapicFragment createInstance() {
        return new ZapicFragment();
    }

    /**
     * Gets the Zapic JavaScript application.
     *
     * @return The Zapic JavaScript application or {@code null} if called before the
     *         {@link #onCreate(Bundle)} lifecycle method or after the {@link #onDestroy()}
     *         lifecycle method.
     */
    @CheckResult
    @MainThread
    @Nullable
    App getApp() {
        return this.mAppManager == null ? null : this.mAppManager.mApp;
    }

    @MainThread
    @Override
    public void onAttach(final Context context) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onAttach");
        }

        super.onAttach(context);

        if (this.mAppManager != null) {
            this.mAppManager.onAttached(this.getActivity());

            if (this.mConnectivityBroadcastReceiver == null) {
                this.mConnectivityBroadcastReceiver = ConnectivityManagerUtilities.registerConnectivityBroadcastReceiver(this.getActivity(), this.mAppManager.mApp);
                this.mConnectivityBroadcastReceiver.notify(this.getActivity());
            }
        }
    }

    @MainThread
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreate");
        }

        super.onCreate(savedInstanceState);

        // This retains the fragment instance when configuration changes occur. This effectively
        // keeps instance variables from being garbage collected. Note that the fragment is still
        // detached from the old activity and attached to the new activity.
        this.setRetainInstance(true);

        assert this.mAppManager == null : "mAppManager != null";
        this.mAppManager = AppManager.getInstance();
        this.mAppManager.onCreated(this.getActivity());

        assert this.mConnectivityBroadcastReceiver == null : "mConnectivityBroadcastReceiver != null";
        this.mConnectivityBroadcastReceiver = ConnectivityManagerUtilities.registerConnectivityBroadcastReceiver(this.getActivity(), this.mAppManager.mApp);
        this.mConnectivityBroadcastReceiver.notify(this.getActivity());
    }

    @MainThread
    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onDestroy");
        }

        super.onDestroy();

        assert this.mAppManager != null : "mAppManager == null";
        this.mAppManager.onDestroyed(this.getActivity());
        this.mAppManager = null;

        assert this.mConnectivityBroadcastReceiver != null : "mConnectivityBroadcastReceiver == null";
        ConnectivityManagerUtilities.unregisterConnectivityBroadcastReceiver(this.getActivity(), this.mConnectivityBroadcastReceiver);
        this.mConnectivityBroadcastReceiver = null;
    }

    @MainThread
    @Override
    public void onDetach() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onDetach");
        }

        super.onDetach();

        if (this.mConnectivityBroadcastReceiver != null) {
            ConnectivityManagerUtilities.unregisterConnectivityBroadcastReceiver(this.getActivity(), this.mConnectivityBroadcastReceiver);
            this.mConnectivityBroadcastReceiver = null;
        }

        if (this.mAppManager != null) {
            this.mAppManager.onDetached(this.getActivity());
        }
    }

    //region Lifecycle Events

    // onAttach

    // onCreate

    @CheckResult
    @MainThread
    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreateView");
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @MainThread
    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onActivityCreated");
        }

        super.onActivityCreated(savedInstanceState);
    }

    @MainThread
    @Override
    public void onStart() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onStart");
        }

        super.onStart();
    }

    @MainThread
    @Override
    public void onResume() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onResume");
        }

        super.onResume();
    }

    @MainThread
    @Override
    public void onPause() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onPause");
        }

        super.onPause();
    }

    @MainThread
    @Override
    public void onStop() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onStop");
        }

        super.onStop();
    }

    @MainThread
    @Override
    public void onDestroyView() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onDestroyView");
        }

        super.onDestroyView();
    }

    // onDestroy

    // onDetach

    //endregion
}
