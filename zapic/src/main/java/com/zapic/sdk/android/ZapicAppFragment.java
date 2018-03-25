package com.zapic.sdk.android;

import android.content.MutableContextWrapper;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;

public final class ZapicAppFragment extends Fragment {
    /**
     * The tag used to identify log messages.
     */
    @NonNull
    private static final String TAG = "ZapicAppFragment";

    /**
     * The {@link WebView}.
     */
    @Nullable
    private WebView mWebView;

    /**
     * Creates a new {@link ZapicAppFragment} instance.
     */
    public ZapicAppFragment() {
        this.mWebView = null;
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreateView");
        }

        final WebViewManager webViewManager = WebViewManager.getInstance();

        assert this.mWebView == null : "mWebView is not null";
        this.mWebView = webViewManager.getWebView();
        final FrameLayout.LayoutParams webViewLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        this.mWebView.setLayoutParams(webViewLayoutParams);

        final FragmentActivity activity = this.getActivity();
        assert activity != null : "activity is null";

        final MutableContextWrapper webViewContext = (MutableContextWrapper) this.mWebView.getContext();
        webViewContext.setBaseContext(activity);

        final View view = inflater.inflate(R.layout.fragment_zapic_app, container, false);

        final FrameLayout frameLayout = view.findViewById(R.id.fragment_zapic_app_container);
        frameLayout.addView(this.mWebView);

        return view;
    }

    @Override
    public void onDestroyView() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreateView");
        }

        assert this.mWebView != null : "mWebView is null";
        final FrameLayout frameLayout = (FrameLayout) this.mWebView.getParent();
        frameLayout.removeView(this.mWebView);

        final FragmentActivity activity = this.getActivity();
        assert activity != null : "activity is null";

        final MutableContextWrapper webViewContext = (MutableContextWrapper) this.mWebView.getContext();
        webViewContext.setBaseContext(activity.getApplicationContext());

        this.mWebView = null;

        super.onDestroyView();
    }
}
