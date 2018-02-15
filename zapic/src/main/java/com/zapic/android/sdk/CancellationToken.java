package com.zapic.android.sdk;

/**
 * Represents a token that can signal cancellation of a long-running task.
 *
 * @author Kyle Dodson
 * @since 1.0.0
 */
interface CancellationToken {
    /**
     * Gets a value indicating whether the signal has been set.
     *
     * @return {@code true} if the signal has been set; {@code false} if the signal has not been
     *         set.
     */
    boolean isCancelled();
}
