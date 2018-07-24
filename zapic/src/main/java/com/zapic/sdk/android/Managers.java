package com.zapic.sdk.android;

import android.support.annotation.NonNull;

/**
 * Provides references to the {@link SessionManager}, {@link ViewManager}, and
 * {@link WebViewManager} instances.
 *
 * @author Kyle Dodson
 * @since 1.2.1
 */
final class Managers {
    /**
     * The {@link SessionManager} instance.
     */
    @NonNull
    private final SessionManager mSessionManager;

    /**
     * The {@link ViewManager} instance.
     */
    @NonNull
    private final ViewManager mViewManager;

    /**
     * The {@link WebViewManager} instance.
     */
    @NonNull
    private final WebViewManager mWebViewManager;

    /**
     * Creates a new {@link Managers} instance.
     *
     * @param sessionManager The {@link SessionManager} instance.
     * @param viewManager    The {@link ViewManager} instance.
     * @param webViewManager The {@link WebViewManager} instance.
     */
    Managers(@NonNull final SessionManager sessionManager, @NonNull final ViewManager viewManager, @NonNull final WebViewManager webViewManager) {
        mSessionManager = sessionManager;
        mViewManager = viewManager;
        mWebViewManager = webViewManager;
    }

    /**
     * Gets the {@link SessionManager} instance.
     *
     * @return The {@link SessionManager} instance.
     */
    @NonNull
    SessionManager getSessionManager() {
        return mSessionManager;
    }

    /**
     * Gets the {@link ViewManager} instance.
     *
     * @return The {@link ViewManager} instance.
     */
    @NonNull
    ViewManager getViewManager() {
        return mViewManager;
    }

    /**
     * Gets the {@link WebViewManager} instance.
     *
     * @return The {@link WebViewManager} instance.
     */
    @NonNull
    WebViewManager getWebViewManager() {
        return mWebViewManager;
    }
}
