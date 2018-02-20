package com.zapic.android.sdk;

import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.transition.Slide;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

/**
 * A {@link Fragment} that presents a loading page.
 * <p>
 * Use the {@link #createInstance()} factory method to create instances of this fragment.
 * <p>
 * Activities that present this fragment must implement the {@link InteractionListener} interface.
 *
 * @author Kyle Dodson
 * @since 1.0.0
 */
public final class LoadingPageFragment extends Fragment {
    /**
     * Handles {@link LoadingPageFragment} interaction events.
     *
     * @author Kyle Dodson
     * @since 1.0.0
     */
    public interface InteractionListener {
        /**
         * Closes the page.
         */
        @MainThread
        void close();
    }

    /**
     * The tag used to identify log messages.
     */
    @NonNull
    private static final String TAG = "LoadingPageFragment";

    /**
     * The interaction listener.
     */
    @Nullable
    private InteractionListener mListener;

    /**
     * Creates a new {@link LoadingPageFragment} instance.
     */
    @MainThread
    public LoadingPageFragment() {
    }

    /**
     * Creates a new {@link LoadingPageFragment} instance.
     *
     * @return The new {@link LoadingPageFragment} instance.
     */
    @CheckResult
    @MainThread
    @NonNull
    public static LoadingPageFragment createInstance() {
        return new LoadingPageFragment();
    }

    @MainThread
    @Override
    public void onAttach(final Context context) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onAttach");
        }

        super.onAttach(context);

        if (context instanceof InteractionListener) {
            this.mListener = (InteractionListener) context;
        } else {
            throw new RuntimeException(String.format("%s must implement InteractionListener", context.toString()));
        }
    }

    @MainThread
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
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

        return inflater.inflate(R.layout.fragment_page_loading, container, false);
    }

    @MainThread
    @Override
    public void onDetach() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onDetach");
        }

        super.onDetach();

        this.mListener = null;
    }

    @MainThread
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onViewCreated");
        }

        super.onViewCreated(view, savedInstanceState);

        ImageButton closeButton = view.findViewById(R.id.fragment_page_loading_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InteractionListener listener = LoadingPageFragment.this.mListener;
                if (listener != null) {
                    listener.close();
                }
            }
        });
    }
}
