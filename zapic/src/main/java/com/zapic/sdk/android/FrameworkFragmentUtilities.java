package com.zapic.sdk.android;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Build;
import android.support.annotation.CheckResult;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Provides utility methods to attach and detach {@link ZapicFrameworkFragment} instances.
 *
 * @author Kyle Dodson
 * @since 1.2.0
 */
final class FrameworkFragmentUtilities {
    /**
     * The tag used to identify {@link ZapicFrameworkFragment} instances.
     */
    @NonNull
    private static final String TAG = "ZAPIC";

    /**
     * Attaches a new {@link ZapicFrameworkFragment} instance to the specified activity.
     *
     * @param activity The activity.
     */
    @MainThread
    static void attach(@NonNull final Activity activity) {
        final FragmentManager fragmentManager = activity.getFragmentManager();
        final Fragment fragment = fragmentManager.findFragmentByTag(TAG);
        if (fragment == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fragmentManager.beginTransaction()
                        .add(new ZapicFrameworkFragment(), TAG)
                        .commitNowAllowingStateLoss();
            } else {
                fragmentManager.beginTransaction()
                        .add(new ZapicFrameworkFragment(), TAG)
                        .commitAllowingStateLoss();
                fragmentManager.executePendingTransactions();
            }
        }
    }

    /**
     * Detaches an existing {@link ZapicFrameworkFragment} instance from the specified activity.
     *
     * @param activity The activity.
     */
    @MainThread
    static void detach(@NonNull final Activity activity) {
        final FragmentManager fragmentManager = activity.getFragmentManager();
        final Fragment fragment = fragmentManager.findFragmentByTag(TAG);
        if (fragment != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fragmentManager.beginTransaction()
                        .remove(fragment)
                        .commitNowAllowingStateLoss();
            } else {
                fragmentManager.beginTransaction()
                        .remove(fragment)
                        .commitAllowingStateLoss();
                fragmentManager.executePendingTransactions();
            }
        }
    }

    /**
     * Tries to cast the specified object to a {@link Fragment} and returns the activity.
     *
     * @param obj The object.
     * @return The activity or {@code null}.
     */
    @CheckResult
    @MainThread
    @Nullable
    static Activity tryGetActivity(@NonNull final Object obj) {
        if (obj instanceof Fragment) {
            final Fragment fragment = (Fragment) obj;
            return fragment.getActivity();
        }

        return null;
    }
}
