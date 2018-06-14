package com.zapic.sdk.android;

import android.support.annotation.AnyThread;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;

import java.util.Map;

/**
 * A Zapic web page.
 *
 * @author Kyle Dodson
 * @since 1.2.0
 */
final class WebPage {
    /**
     * The collection of headers.
     */
    @NonNull
    private final Map<String, String> mHeaders;

    /**
     * The HTML.
     */
    @NonNull
    private final String mHtml;

    /**
     * The last cache validation date and time.
     */
    private final long mLastValidated;

    /**
     * Creates a new instance.
     *
     * @param html          The HTML.
     * @param headers       The collection of headers.
     * @param lastValidated The last cache validation date and time or {@code 0} if it has not been
     *                      validated.
     */
    @AnyThread
    WebPage(@NonNull final String html, @NonNull final Map<String, String> headers, long lastValidated) {
        mHeaders = headers;
        mHtml = html;
        mLastValidated = lastValidated;
    }

    /**
     * Gets the collection of headers.
     *
     * @return The collection of headers.
     */
    @AnyThread
    @CheckResult
    @NonNull
    Map<String, String> getHeaders() {
        return mHeaders;
    }

    /**
     * Gets the HTML.
     *
     * @return The HTML.
     */
    @AnyThread
    @CheckResult
    @NonNull
    String getHtml() {
        return mHtml;
    }

    /**
     * Gets the last cache validation date and time in milliseconds since January 1, 1970 or
     * {@code 0} if it has not been validated.
     *
     * @return The last cache validation date and time in milliseconds since January 1, 1970 or
     * {@code 0} if it has not been validated.
     */
    @AnyThread
    @CheckResult
    long getLastValidated() {
        return mLastValidated;
    }
}
