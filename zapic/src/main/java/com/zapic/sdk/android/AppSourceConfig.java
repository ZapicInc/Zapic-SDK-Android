package com.zapic.sdk.android;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;

/**
 * A utility class that overrides the behavior of loading the Zapic JavaScript application source
 * for debug purposes.
 * <p>
 * <em>These settings will break production apps. You should only use these settings when
 * debugging apps with Zapic support engineers.</em>
 *
 * @author Kyle Dodson
 * @since 1.0.4
 */
@SuppressWarnings("DeprecatedIsStillUsed")
public final class AppSourceConfig {
    /**
     * The Zapic application source URL.
     */
    @NonNull
    private static final String APP_SOURCE_URL = "https://app.zapic.net";

    /**
     * A value indicating whether to use a cached application source.
     */
    private static boolean mCache = true;

    @NonNull
    private static String mUrl = APP_SOURCE_URL;

    /**
     * Disables using a cached application source.
     */
    @Deprecated
    public static void disableCache() {
        mCache = false;
    }

    /**
     * Enables using a cached application source.
     */
    @Deprecated
    public static void enableCache() {
        mCache = true;
    }

    /**
     * Gets the application source URL.
     *
     * @return The application source URL.
     */
    @CheckResult
    @Deprecated
    @NonNull
    public static String getUrl() {
        return mUrl;
    }

    /**
     * Gets a value indicating whether to use a cached application source.
     *
     * @return <code>true</code> to use a cached application source; <code>false</code> to not use a
     * cached application source.
     */
    @CheckResult
    @Deprecated
    public static boolean isCacheEnabled() {
        return mCache;
    }

    /**
     * Sets the application source URL.
     *
     * @param url The application source URL.
     */
    @Deprecated
    public static void setUrl(@NonNull String url) {
        mUrl = url;
    }
}
