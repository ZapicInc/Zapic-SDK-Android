package com.zapic.sdk.android;

import android.support.annotation.NonNull;

/**
 * A representation of the Zapic JavaScript application source and version.
 *
 * @author Kyle Dodson
 * @since 1.0.0
 */
final class AppSource {
    /**
     * The ETag.
     */
    @NonNull
    private final String eTag;

    /**
     * The HTML source.
     */
    @NonNull
    private final String html;

    /**
     * The Last-Modified date and time.
     */
    private final long lastModified;

    /**
     * The last cache validation date and time.
     */
    private final long lastValidated;

    /**
     * Creates a new instance.
     *
     * @param html          The HTML source.
     * @param eTag          The ETag or an empty string if one was not returned in the HTTP
     *                      response.
     * @param lastModified  The Last-Modified date and time or {@code 0} if one was not returned in
     *                      the HTTP response.
     * @param lastValidated The last cache validation date and time or {@code 0} if it has not been
     *                      validated.
     */
    AppSource(@NonNull final String html, @NonNull final String eTag, long lastModified, long lastValidated) {
        this.eTag = eTag;
        this.html = html;
        this.lastModified = lastModified;
        this.lastValidated = lastValidated;
    }

    /**
     * Gets the ETag.
     *
     * @return The ETag or an empty string if one was not returned in the HTTP response.
     */
    @NonNull
    String getETag() {
        return this.eTag;
    }

    /**
     * Gets the HTML source.
     *
     * @return The HTML source.
     */
    @NonNull
    String getHtml() {
        return this.html;
    }

    /**
     * Gets the Last-Modified date and time in milliseconds since January 1, 1970.
     *
     * @return The Last-Modified date and time in milliseconds since January 1, 1970 or {@code 0}
     * if one was not returned in the HTTP response.
     */
    long getLastModified() {
        return this.lastModified;
    }

    /**
     * Gets the last cache validation date and time in milliseconds since January 1, 1970 or
     * {@code 0} if it has not been validated.
     *
     * @return The last cache validation date and time in milliseconds since January 1, 1970 or
     * {@code 0} if it has not been validated.
     */
    long getLastValidated() {
        return this.lastValidated;
    }
}
