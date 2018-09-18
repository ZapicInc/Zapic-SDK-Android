package com.zapic.sdk.android;

import android.graphics.Bitmap;
import android.support.annotation.AnyThread;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

/**
 * Represents a notification message.
 *
 * @author Kyle Dodson
 * @since 1.2.0
 */
final class Notification {
    /**
     * The image.
     */
    @Nullable
    private final Bitmap mImage;

    /**
     * The metadata.
     */
    @Nullable
    private final JSONObject mMetadata;

    /**
     * The text.
     */
    @Nullable
    private final String mText;

    /**
     * The title.
     */
    @NonNull
    private final String mTitle;

    /**
     * Creates a new {@link Notification} instance.
     *
     * @param title    The title.
     * @param text     The text. If non-{@code null}, this text is rendered smaller and below
     *                 {@code title}.
     * @param image    The image. If non-{@code null}, this image is rendered at the start of the
     *                 view. If {@code null}, the Zapic logo is rendered at the start of the view.
     * @param metadata The metadata. If non-{@code null}, dispatches an interaction event when the
     *                 notification message is tapped.
     */
    @AnyThread
    Notification(@NonNull final String title, @Nullable final String text, @Nullable final Bitmap image, @Nullable final JSONObject metadata) {
        mImage = image;
        mMetadata = metadata;
        mText = text;
        mTitle = title;
    }

    /**
     * Gets the image.
     * <p>
     * If non-{@code null}, this image is rendered at the start of the view. If {@code null}, the
     * Zapic logo is rendered at the start of the view.
     *
     * @return The image or {@code null}.
     */
    @AnyThread
    @CheckResult
    @Nullable
    Bitmap getImage() {
        return mImage;
    }

    /**
     * Gets the metadata.
     * <p>
     * If non-{@code null}, dispatches an interaction event when the notification message is tapped.
     *
     * @return The metadata or {@code null}.
     */
    @AnyThread
    @CheckResult
    @Nullable
    public JSONObject getMetadata() {
        return mMetadata;
    }

    /**
     * Gets the text.
     * <p>
     * If non-{@code null}, this text is rendered smaller and below the title.
     *
     * @return The text or {@code null}.
     */
    @AnyThread
    @CheckResult
    @Nullable
    String getText() {
        return mText;
    }

    /**
     * Gets the title.
     *
     * @return The title.
     */
    @AnyThread
    @CheckResult
    @NonNull
    String getTitle() {
        return mTitle;
    }
}
