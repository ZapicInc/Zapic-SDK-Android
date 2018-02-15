package com.zapic.android.sdk;

import android.app.Fragment;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

/**
 * A {@link Fragment} that runs the Zapic application in the background.
 * <p>
 * Use the {@link ZapicFragment#createInstance} factory method to create instances of this fragment.
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
public final class ZapicFragment extends Fragment {
    /**
     * The tag used to identify log messages.
     */
    @NonNull
    private static final String TAG = "ZapicFragment";

    /**
     * The web client application manager.
     */
    @Nullable
    private AppManager mAppManager;

    /**
     * The connectivity change event receiver.
     */
    @Nullable
    private ConnectivityReceiver mConnectivityReceiver;

    /**
     * Creates a new instance.
     */
    public ZapicFragment() {
        this.mAppManager = null;
        this.mConnectivityReceiver = null;
    }

    /**
     * Creates a new instance of the {@link ZapicFragment} class.
     *
     * @return The new instance of the {@link ZapicFragment} class.
     */
    @CheckResult
    @MainThread
    @NonNull
    public static ZapicFragment createInstance() {
        return new ZapicFragment();
    }

    @MainThread
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        // This "recycles" the fragment instance when configuration changes occur allowing instance
        // variables to be retained. Note that the fragment is detached from the old activity and
        // then attached to the new activity.
        this.setRetainInstance(true);

        // Get a reference to the AppManager (to keep it from being garbage collected).
        if (this.mAppManager == null) {
            this.mAppManager = AppManager.getInstance();
        }

        // Create the WebView (if this is the first view).
        this.mAppManager.onViewCreated(this.getActivity());

        // Register to start receiving connectivity change events.
        assert this.mConnectivityReceiver == null : "mConnectivityReceiver != null";
        IntentFilter connectivityFilter = new IntentFilter();
        connectivityFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        this.mConnectivityReceiver = new ConnectivityReceiver(this.mAppManager);
        this.getActivity().registerReceiver(this.mConnectivityReceiver, connectivityFilter);
    }

    @MainThread
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();

        // Unregister to stop receiving connectivity change events.
        assert this.mConnectivityReceiver != null : "mConnectivityReceiver == null";
        this.getActivity().unregisterReceiver(this.mConnectivityReceiver);
        this.mConnectivityReceiver = null;

        // Destroy the WebView (if this is the last view).
        assert this.mAppManager != null : "mAppManager == null";
        this.mAppManager.onViewDestroyed();
        this.mAppManager = null;
    }

    @MainThread
    @Override
    public void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();

        // Check initial connectivity state.
        final ConnectivityManager connectivityManager = (ConnectivityManager)this.getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connectivityManager != null : "connectivityManager == null";

        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        assert this.mAppManager != null : "mAppManager == null";
        if (networkInfo != null && networkInfo.isConnected()) {
            this.mAppManager.setOnline();
        } else {
            this.mAppManager.setOffline();
        }
    }

    //region Lifecycle Events

    @MainThread
    @Override
    public void onAttach(final Context context) {
        Log.d(TAG, "onAttach");
        super.onAttach(context);
    }

    // onCreate

    @CheckResult
    @MainThread
    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @MainThread
    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
    }

    // onStart

    @MainThread
    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
    }

    @MainThread
    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @MainThread
    @Override
    public void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @MainThread
    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView");
        super.onDestroyView();
    }

    // onDestroy

    @MainThread
    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach");
        super.onDetach();
    }

    //endregion
}
