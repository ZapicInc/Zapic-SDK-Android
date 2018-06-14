package com.zapic.sdk.android;

import android.app.Activity;
import android.support.annotation.CheckResult;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

/**
 * Provides utility methods to attach and detach {@link ZapicSupportFragment} instances.
 *
 * @author Kyle Dodson
 * @since 1.2.0
 */
final class SupportFragmentUtilities {
    /**
     * The tag used to identify {@link ZapicSupportFragment} instances.
     */
    @NonNull
    private static final String TAG = "ZAPIC";

    /**
     * A value indicating whether the support fragment library has been loaded.
     * <p>
     * This is {@code null} before the first attempt to load the support fragment library. This is
     * {@code true} if the support fragment library has successfully been loaded. This is
     * {@code false} if the support fragment library could not be loaded.
     */
    @Nullable
    private static Boolean sSupportLibraryLoaded = null;

    /**
     * Detaches an existing {@link ZapicSupportFragment} instance from the specified activity.
     *
     * @param activity The activity.
     */
    @MainThread
    static void detach(@NonNull final Activity activity) {
        final FragmentManager fragmentManager = tryGetFragmentManager(activity);
        if (fragmentManager != null) {
            final Fragment fragment = fragmentManager.findFragmentByTag(TAG);
            if (fragment != null) {
                fragmentManager.beginTransaction()
                        .remove(fragment)
                        .commitNowAllowingStateLoss();
            }
        }
    }

    /**
     * Attaches a new {@link ZapicSupportFragment} instance to the specified activity if it is a
     * {@link FragmentActivity} instance.
     *
     * @param activity The activity.
     * @return {@code true} if a fragment has successfully been attached; otherwise, {@code false}.
     */
    @MainThread
    static boolean tryAttach(@NonNull final Activity activity) {
        final FragmentManager fragmentManager = tryGetFragmentManager(activity);
        if (fragmentManager != null) {
            final Fragment fragment = fragmentManager.findFragmentByTag(TAG);
            if (fragment == null) {
                fragmentManager.beginTransaction()
                        .add(new ZapicSupportFragment(), TAG)
                        .commitNowAllowingStateLoss();
            }

            return true;
        }

        return false;
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
        if (sSupportLibraryLoaded == null) {
            try {
                final Class<?> clazz = Class.forName("android.support.v4.app.Fragment");
                sSupportLibraryLoaded = clazz != null;
            } catch (ClassNotFoundException e) {
                sSupportLibraryLoaded = false;
            }
        }

        if (sSupportLibraryLoaded && obj instanceof Fragment) {
            final Fragment fragment = (Fragment) obj;
            return fragment.getActivity();
        }

        return null;
    }

    /**
     * Tries to get a {@link FragmentManager} instance from the specified activity.
     *
     * @param activity The activity.
     * @return The {@link FragmentManager} instance or {@code null}.
     */
    @CheckResult
    @MainThread
    @Nullable
    private static FragmentManager tryGetFragmentManager(@NonNull final Activity activity) {
        if (sSupportLibraryLoaded == null) {
            try {
                final Class<?> clazz = Class.forName("android.support.v4.app.Fragment");
                sSupportLibraryLoaded = clazz != null;
            } catch (ClassNotFoundException e) {
                sSupportLibraryLoaded = false;
            }
        }

        if (sSupportLibraryLoaded && activity instanceof FragmentActivity) {
            final FragmentActivity fragmentActivity = (FragmentActivity) activity;
            return fragmentActivity.getSupportFragmentManager();
        }

        return null;
    }
}
