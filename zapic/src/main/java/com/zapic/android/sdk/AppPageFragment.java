package com.zapic.android.sdk;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.transition.Slide;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.view.ViewParent;
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
     * The {@link WebView}.
     */
    @Nullable
    private WebView mWebView;

    /**
     * Creates a new {@link AppPageFragment} instance.
     */
    public AppPageFragment() {
        this.mWebView = null;
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
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreate");
        }

        super.onCreate(savedInstanceState);

        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final Slide slide = new Slide();
            slide.setDuration(500);
            this.setEnterTransition(slide);
        }
    }

    @MainThread
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreateView");
        }

        View view = inflater.inflate(R.layout.fragment_page_app, container, false);

        if (this.mWebView != null) {
            final FrameLayout.LayoutParams webViewLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            this.mWebView.setLayoutParams(webViewLayoutParams);

            final FrameLayout frameLayout = view.findViewById(R.id.fragment_page_app_container);
            frameLayout.addView(this.mWebView);
        }

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onDestroyView");
        }

        super.onActivityCreated(savedInstanceState);

        if (this.mWebView == null) {
            final Activity activity = this.getActivity();
            if (activity instanceof ZapicActivity) {
                final ZapicActivity zapicActivity = (ZapicActivity)activity;
                final App app = zapicActivity.getApp();
                if (app == null) {
                    zapicActivity.openLoadingPage();
                } else {
                    final WebView webView = app.getWebView();
                    if (webView == null) {
                        zapicActivity.openLoadingPage();
                    } else {
                        this.mWebView = webView;

                        final FrameLayout.LayoutParams webViewLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
                        this.mWebView.setLayoutParams(webViewLayoutParams);

                        View view = this.getView();
                        assert view != null : "view == null";

                        final FrameLayout frameLayout = view.findViewById(R.id.fragment_page_app_container);
                        frameLayout.addView(this.mWebView);
                    }
                }
            }
        }
    }

    @MainThread
    @Override
    public void onDestroyView() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onDestroyView");
        }

        if (this.mWebView != null) {
            ViewParent parent = this.mWebView.getParent();
            if (parent instanceof ViewManager) {
                ((ViewManager)parent).removeView(this.mWebView);
            }

            this.mWebView = null;
        }

        super.onDestroyView();
    }

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "onAttach");

        super.onAttach(context);

        final Activity activity = this.getActivity();
        if (activity instanceof ZapicActivity) {
            final ZapicActivity zapicActivity = (ZapicActivity)activity;
            final App app = zapicActivity.getApp();
            if (app != null) {
                final WebView webView = app.getWebView();
                if (webView != null) {
                    this.mWebView = webView;
                }
            }
        }
    }
}
