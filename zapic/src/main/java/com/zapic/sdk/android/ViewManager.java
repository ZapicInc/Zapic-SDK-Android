package com.zapic.sdk.android;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.zapic.sdk.android.alerter.Alerter;
import com.zapic.sdk.android.alerter.OnHideAlertListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Manages the user interface components.
 * <p>
 * This facilitates routing of intents by maintaining the {@link ZapicActivity} reference. This also
 * coordinates passing the {@link WebView} instance to the {@link ZapicActivity} instance.
 * <p>
 * This facilitates routing of notification messages to the topmost activity by maintaining a stack
 * of fragment references ({@link ZapicFrameworkFragment} and {@link ZapicSupportFragment} types).
 * This automatically queues notification messages when another notification message has already
 * been shown.
 *
 * @author Kyle Dodson
 * @since 1.2.0
 */
@MainThread
final class ViewManager {
    /**
     * The tag used to identify log messages.
     */
    @NonNull
    private static final String TAG = "ViewManager";

    /**
     * The notification message event listener.
     */
    @NonNull
    private final AlertListener mAlertListener;

    /**
     * The queue of notification messages.
     */
    @NonNull
    private final LinkedList<Notification> mNotifications;

    /**
     * The {@link ZapicActivity} instance.
     */
    @Nullable
    private ZapicActivity mActivity;

    /**
     * The list of current {@link ZapicFrameworkFragment} and {@link ZapicSupportFragment}
     * instances.
     */
    @NonNull
    private List<Object> mFragments;

    /**
     * The current page of the {@link ZapicActivity} instance.
     */
    @Nullable
    private Page mPage;

    /**
     * The {@link WebView} instance.
     */
    @Nullable
    private WebView mWebView;

    /**
     * Creates a new {@link ViewManager} instance.
     */
    @MainThread
    ViewManager() {
        mActivity = null;
        mAlertListener = new AlertListener();
        mFragments = new ArrayList<>();
        mNotifications = new LinkedList<>();
        mPage = Page.LOADING_PAGE;
        mWebView = null;
    }

    /**
     * Attaches a {@link ZapicFrameworkFragment} or {@link ZapicSupportFragment} instance to the
     * specified activity.
     *
     * @param activity The activity.
     */
    @MainThread
    void attachFragment(@NonNull final Activity activity) {
        if (!SupportFragmentUtilities.tryAttach(activity)) {
            FrameworkFragmentUtilities.attach(activity);
        }
    }

    /**
     * Detaches a {@link ZapicFrameworkFragment} or {@link ZapicSupportFragment} instance from the
     * specified activity.
     *
     * @param activity The activity.
     */
    @MainThread
    void detachFragment(@NonNull final Activity activity) {
        SupportFragmentUtilities.detach(activity);
        FrameworkFragmentUtilities.detach(activity);
    }

    /**
     * Dispatches a message to the Zapic web page.
     *
     * @param message The message.
     */
    @MainThread
    private void dispatch(@NonNull final JSONObject message) {
        assert mWebView != null : "mWebView == null";
        mWebView.evaluateJavascript("window.zapic.dispatch(" + message.toString() + ")", null);
    }

    /**
     * Dispatches a "CLOSE_PAGE" message to the Zapic web page.
     */
    @MainThread
    private void dispatchClosePage() {
        if (mWebView != null) {
            try {
                dispatch(new JSONObject().put("type", "CLOSE_PAGE"));
            } catch (JSONException ignored) {
            }
        }
    }

    /**
     * Dispatches an "OPEN_PAGE" message to the Zapic web page.
     *
     * @param page The page to show.
     * @see ZapicPages
     */
    @MainThread
    private void dispatchOpenPage(@NonNull final String page) {
        if (mWebView != null) {
            try {
                dispatch(new JSONObject().put("type", "OPEN_PAGE").put("payload", page));
            } catch (JSONException ignored) {
            }
        }
    }

    /**
     * Hides the {@link ZapicActivity} instance.
     */
    @MainThread
    void hideWebPage() {
        if (mActivity != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mActivity.finishAfterTransition();
            } else {
                mActivity.finish();
            }
        }
    }

    /**
     * Called when the {@link ZapicActivity} is created. This dispatches an "OPEN_PAGE" message to
     * the Zapic web page if the {@link WebView} is loaded.
     *
     * @param activity The activity.
     */
    @MainThread
    void onActivityCreated(@NonNull final ZapicActivity activity) {
        if (mActivity != null) {
            final ZapicActivity oldActivity = mActivity;
            mActivity = null;

            Log.w(TAG, "Finishing existing ZapicActivity instance");
            oldActivity.showLoadingPage();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                oldActivity.finishAfterTransition();
            } else {
                oldActivity.finish();
            }
        }

        mActivity = activity;
        if (mPage == Page.LOADING_PAGE) {
            activity.showLoadingPage();
        } else if (mPage == Page.RETRY_PAGE) {
            activity.showRetryPage();
        } else {
            activity.showLoadingPage();
            if (mWebView == null) {
                mPage = Page.LOADING_PAGE;
            } else {
                final String page = getPageFromActivityIntent(activity);
                dispatchOpenPage(page);
            }
        }
    }

    /**
     * Called when the {@link ZapicActivity} is destroyed.
     *
     * @param activity The activity.
     */
    @MainThread
    void onActivityDestroyed(@NonNull final ZapicActivity activity) {
        if (mActivity == activity) {
            dispatchClosePage();
            mActivity = null;
        }
    }

    /**
     * Called when the {@link ZapicActivity} intent is updated. This dispatches an "OPEN_PAGE"
     * message to the Zapic web page if the {@link WebView} is loaded.
     *
     * @param activity The activity.
     */
    @MainThread
    void onActivityUpdated(@NonNull final ZapicActivity activity) {
        if (mActivity == activity && mWebView != null) {
            final String page = getPageFromActivityIntent(activity);
            dispatchOpenPage(page);
        }
    }

    /**
     * Called when a {@link ZapicFrameworkFragment} instance pauses. This removes the fragment from
     * the stack.
     *
     * @param fragment The fragment.
     */
    @MainThread
    void onPause(@NonNull final ZapicFrameworkFragment fragment) {
        final ArrayList<Object> fragments = new ArrayList<>();
        for (final Object reference : mFragments) {
            if (fragment != reference) {
                fragments.add(reference);
            }
        }

        mFragments = fragments;
    }

    /**
     * Called when a {@link ZapicSupportFragment} instance pauses. This removes the fragment from
     * the stack.
     *
     * @param fragment The fragment.
     */
    @MainThread
    void onPause(@NonNull final ZapicSupportFragment fragment) {
        final ArrayList<Object> fragments = new ArrayList<>();
        for (final Object reference : mFragments) {
            if (fragment != reference) {
                fragments.add(reference);
            }
        }

        mFragments = fragments;
    }

    /**
     * Called when a {@link ZapicFrameworkFragment} instance resumes. This adds the fragment to the
     * top of the stack.
     *
     * @param fragment The fragment.
     */
    @MainThread
    void onResume(@NonNull final ZapicFrameworkFragment fragment) {
        final ArrayList<Object> fragments = new ArrayList<>();
        fragments.add(fragment);
        for (final Object reference : mFragments) {
            if (fragment != reference) {
                fragments.add(reference);
            }
        }

        mFragments = fragments;
        showNotificationIfReady();
    }

    /**
     * Called when a {@link ZapicSupportFragment} instance resumes. This adds the fragment to the
     * top of the stack.
     *
     * @param fragment The fragment.
     */
    @MainThread
    void onResume(@NonNull final ZapicSupportFragment fragment) {
        final ArrayList<Object> fragments = new ArrayList<>();
        fragments.add(fragment);
        for (final Object reference : mFragments) {
            if (fragment != reference) {
                fragments.add(reference);
            }
        }

        mFragments = fragments;
        showNotificationIfReady();
    }

    /**
     * Called when the {@link WebView} instance has crashed.
     */
    @MainThread
    void onWebViewCrashed() {
        mWebView = null;
        mPage = Page.LOADING_PAGE;

        if (mActivity != null) {
            mActivity.showLoadingPage();
        }
    }

    /**
     * Called when the {@link WebView} instance has loaded the Zapic web page.
     *
     * @param webView The {@link WebView} instance.
     */
    @MainThread
    void onWebViewLoaded(@NonNull final WebView webView) {
        mWebView = webView;
        mPage = Page.WEB_PAGE;

        if (mActivity != null) {
            final String page = getPageFromActivityIntent(mActivity);
            dispatchOpenPage(page);
        }
    }

    /**
     * Called when the {@link WebView} instance is ready to be shown.
     */
    @MainThread
    void onWebViewReady() {
        if (mActivity != null) {
            if (mWebView == null) {
                mPage = Page.LOADING_PAGE;
                mActivity.showLoadingPage();
            } else {
                mActivity.showWebPage(mWebView);
            }
        }
    }

    /**
     * Shows an image chooser on the {@link ZapicActivity} instance.
     * <p>
     * This will cancel the image chooser by invoking {@code filePathCallback} with {@code null} if
     * a {@link ZapicActivity} instance does not exist.
     *
     * @param filePathCallback The callback that must be invoked with the chosen file path(s). This
     *                         may be invoked with {@code null} to cancel the image chooser.
     */
    @MainThread
    void showImageChooser(@NonNull final ValueCallback<Uri[]> filePathCallback) {
        if (mActivity == null) {
            filePathCallback.onReceiveValue(null);
        } else {
            mActivity.showImageChooser(filePathCallback);
        }
    }

    /**
     * Shows the loading page on the {@link ZapicActivity} instance.
     */
    @MainThread
    void showLoadingPage() {
        mPage = Page.LOADING_PAGE;

        if (mActivity != null) {
            mActivity.showLoadingPage();
        }
    }

    /**
     * Shows a notification message on the topmost activity.
     * <p>
     * This automatically queues notification messages when another notification message has already
     * been shown.
     *
     * @param notification The notification message.
     */
    @MainThread
    void showNotification(@NonNull final Notification notification) {
        mNotifications.offer(notification);
        showNotificationIfReady();
    }

    /**
     * Shows a notification message on the topmost activity if another notification message has not
     * already been shown.
     */
    @MainThread
    private void showNotificationIfReady() {
        if (Alerter.isShowing()) {
            return;
        }

        final Notification notification = mNotifications.peek();
        if (notification == null) {
            return;
        }

        final Activity activity = getActivityOfTopmostFragment();
        if (activity == null) {
            return;
        }

        Alerter alerter = Alerter.create(activity)
                .enableInfiniteDuration(false)
                .enableSwipeToDismiss()
                .enableVibration(false)
                .setOnHideListener(mAlertListener)
                .setTitle(notification.getTitle());

        final Bitmap image = notification.getImage();
        if (image != null) {
            alerter = alerter.setIcon(image);
        }

        final String text = notification.getText();
        if (text != null) {
            alerter = alerter.setText(text);
        }

        final JSONObject parameters = notification.getParameters();
        if (parameters != null) {
            alerter = alerter.setOnClickListener(mAlertListener);
        }

        alerter.show();
    }

    /**
     * Shows the retry page on the {@link ZapicActivity} instance.
     */
    @MainThread
    void showRetryPage() {
        mPage = Page.RETRY_PAGE;

        if (mActivity != null) {
            mActivity.showRetryPage();
        }
    }

    /**
     * Shows an app chooser to share content on the {@link ZapicActivity} instance.
     * <p>
     * This will ignore the app chooser if a {@link ZapicActivity} instance does not exist.
     *
     * @param intent The intent.
     */
    @MainThread
    void showShareChooser(@NonNull final Intent intent) {
        if (mActivity != null) {
            mActivity.startActivity(Intent.createChooser(intent, "Share"));
        }
    }

    /**
     * Shows the {@link WebView} instance on the {@link ZapicActivity} instance.
     */
    @MainThread
    void showWebPage() {
        mPage = Page.WEB_PAGE;

        final Activity activity = getActivityOfTopmostFragment();
        if (activity != null) {
            activity.startActivity(ZapicActivity.createIntent(activity, "current"));
        }
    }

    /**
     * Gets the activity of the topmost fragment.
     *
     * @return The activity or {@code null} if one does not exist.
     */
    @CheckResult
    @MainThread
    @Nullable
    private Activity getActivityOfTopmostFragment() {
        final Object fragment;
        try {
            fragment = mFragments.get(0);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }

        Activity activity = null;
        if (fragment != null) {
            activity = SupportFragmentUtilities.tryGetActivity(fragment);
            if (activity == null) {
                activity = FrameworkFragmentUtilities.tryGetActivity(fragment);
            }
        }

        return activity;
    }

    /**
     * Gets the value of the "PAGE" extra from the specified activity's intent.
     *
     * @param activity The activity.
     * @return The value of the "PAGE" extra or "default" if it does not exist.
     */
    private String getPageFromActivityIntent(@NonNull final Activity activity) {
        final Intent intent = activity.getIntent();
        final Bundle extras = intent.getExtras();
        if (extras != null) {
            return extras.getString("PAGE", "default");
        } else {
            return "default";
        }
    }

    /**
     * Represents the pages of the {@link ZapicActivity} instance.
     */
    private enum Page {
        /**
         * Identifies the loading page.
         */
        LOADING_PAGE,

        /**
         * Identifies the retry page.
         */
        RETRY_PAGE,

        /**
         * Identifies the {@link WebView} page.
         */
        WEB_PAGE,
    }

    /**
     * A notification message event listener.
     * <p>
     * This dispatches interaction events when a notification message is clicked. This also shows
     * the next notification message in the queue after the current notification message is hidden.
     */
    private class AlertListener implements OnClickListener, OnHideAlertListener {
        @MainThread
        @Override
        public void onClick(@Nullable final View view) {
            final Notification notification = mNotifications.peek();
            if (notification != null) {
                final JSONObject parameters = notification.getParameters();
                if (parameters != null) {
                    Zapic.handleInteraction(parameters);
                }
            }
        }

        @MainThread
        @Override
        public void onHide() {
            mNotifications.poll();
            showNotificationIfReady();
        }
    }
}
