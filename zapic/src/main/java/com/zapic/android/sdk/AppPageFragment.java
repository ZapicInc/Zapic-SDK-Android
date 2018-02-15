package com.zapic.android.sdk;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.webkit.WebView;
import android.widget.FrameLayout;

/**
 * A {@link Fragment} that renders the web client application page.
 * <p>
 * Use the {@link AppPageFragment#createInstance} factory method to create instances of this
 * fragment.
 *
 * @author Kyle Dodson
 * @since 1.0.0
 */
public final class AppPageFragment extends Fragment {
    /**
     * The tag used to identify log messages.
     */
    @NonNull
    private static final String TAG = "AppPageFragment";

    /**
     * Creates a new instance.
     */
    public AppPageFragment() {
    }

    /**
     * Creates a new instance of the {@link AppPageFragment} class.
     *
     * @return The new instance of the {@link AppPageFragment} class.
     */
    @NonNull
    public static AppPageFragment createInstance() {
        return new AppPageFragment();
    }

    @MainThread
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        // This "recycles" the fragment instance when configuration changes occur allowing instance
        // variables to be retained. Note that the fragment is detached from the old activity and
        // then attached to the new activity.
        this.setRetainInstance(true);
    }

    @MainThread
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        final View view = inflater.inflate(R.layout.fragment_page_app, container, false);

        final AppManager appManager = AppManager.getInstance();
        final WebView webView = appManager.getWebView();
        if (webView == null) {
            return view;
        }

        final FrameLayout.LayoutParams webViewLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        webView.setLayoutParams(webViewLayoutParams);

        final FrameLayout frameLayout = view.findViewById(R.id.fragment_page_app_container);
        frameLayout.addView(webView);

        return view;
    }

    @MainThread
    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView");

        final AppManager appManager = AppManager.getInstance();
        final WebView webView = appManager.getWebView();
        if (webView != null) {
            ((ViewManager)webView.getParent()).removeView(webView);
        }

        super.onDestroyView();
    }
}
