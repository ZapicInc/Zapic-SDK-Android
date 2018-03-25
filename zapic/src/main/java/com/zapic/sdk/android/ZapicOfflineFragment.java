package com.zapic.sdk.android;

import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;

public final class ZapicOfflineFragment extends Fragment {
    /**
     * The tag used to identify log messages.
     */
    @NonNull
    private static final String TAG = "ZapicOfflineFragment";

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreateView");
        }

        View view = inflater.inflate(R.layout.fragment_zapic_offline, container, false);

        View appBar = ViewCompat.requireViewById(view, R.id.component_zapic_app_bar);

        FrameLayout gradient = ViewCompat.requireViewById(appBar, R.id.component_zapic_app_bar_gradient);
        AnimationDrawable gradientAnimation = (AnimationDrawable) gradient.getBackground();
        gradientAnimation.setEnterFadeDuration(750);
        gradientAnimation.setExitFadeDuration(750);
        gradientAnimation.start();

        ImageButton closeButton = ViewCompat.requireViewById(appBar, R.id.component_zapic_app_bar_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final FragmentActivity activity = ZapicOfflineFragment.this.getActivity();
                if (activity != null) {
                    activity.finish();
                }
            }
        });

        return view;
    }
}
