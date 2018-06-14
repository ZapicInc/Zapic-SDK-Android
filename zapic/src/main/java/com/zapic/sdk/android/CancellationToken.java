package com.zapic.sdk.android;

/**
 * Represents a signal to cancel a long-running task.
 *
 * @author Kyle Dodson
 * @since 1.0.0
 */
interface CancellationToken {
    /**
     * Gets a value indicating whether the long-running task was cancelled.
     *
     * @return {@code true} if the long-running task was cancelled; {@code false} if the
     * long-running task was not cancelled.
     */
    boolean isCancelled();
}
