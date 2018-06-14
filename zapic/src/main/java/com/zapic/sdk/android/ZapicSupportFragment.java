package com.zapic.sdk.android;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.util.Log;

/**
 * A fragment used to relay intents and notification messages to the topmost activity.
 *
 * @author Kyle Dodson
 * @since 1.2.0
 */
public final class ZapicSupportFragment extends Fragment {
    /**
     * The tag used to identify log messages.
     */
    @NonNull
    private static final String TAG = "ZapicSupportFragment";

    /**
     * The view manager.
     */
    @Nullable
    private ViewManager mViewManager;

    /**
     * Creates a new {@link ZapicSupportFragment} instance.
     */
    @MainThread
    public ZapicSupportFragment() {
        mViewManager = null;
    }

    @MainThread
    @Override
    @SuppressWarnings("deprecation")
    public void onAttach(@NonNull final Activity activity) {
        super.onAttach(activity);

        if (mViewManager == null) {
            mViewManager = Zapic.onAttachedFragment(activity);
        }
    }

    @MainThread
    @Override
    @RequiresApi(Build.VERSION_CODES.M)
    public void onAttach(@NonNull final Context context) {
        super.onAttach(context);

        if (mViewManager == null) {
            mViewManager = Zapic.onAttachedFragment(context);
        }
    }

    @MainThread
    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This retains the fragment instance when configuration changes occur. This effectively
        // keeps instance variables from being garbage collected. Note that the fragment is still
        // detached from the old activity and attached to the new activity.
        setRetainInstance(true);
    }

    @MainThread
    @Override
    public void onPause() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onPause");
        }

        super.onPause();

        if (mViewManager != null) {
            mViewManager.onPause(this);
        }
    }

    @MainThread
    @Override
    public void onResume() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onResume");
        }

        super.onResume();

        if (mViewManager != null) {
            mViewManager.onResume(this);
        }
    }

    @MainThread
    @Override
    public void onStop() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onStop");
        }

        super.onStop();

        mViewManager = null;
    }
}
