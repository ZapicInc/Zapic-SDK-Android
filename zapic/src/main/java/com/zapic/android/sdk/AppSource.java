package com.zapic.android.sdk;

import android.support.annotation.NonNull;

/**
 * A representation of the web client application source and version.
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
     * Creates a new instance.
     *
     * @param html         The HTML source.
     * @param eTag         The ETag or an empty string if one was not returned in the HTTP response.
     * @param lastModified The Last-Modified date and time or {@code 0} if one was not returned in
     *                     the HTTP response.
     */
    AppSource(@NonNull final String html, @NonNull final String eTag, long lastModified) {
        this.eTag = eTag;
        this.html = html;
        this.lastModified = lastModified;
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
}
