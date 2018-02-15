package com.zapic.android.sdk;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * A {@link Fragment} that renders a loading page.
 * <p>
 * Use the {@link LoadingPageFragment#createInstance} factory method to create instances of this
 * fragment.
 * <p>
 * Activities that include this fragment must implement the
 * {@link LoadingPageFragment.InteractionListener} interface.
 *
 * @author Kyle Dodson
 * @since 1.0.0
 */
public final class LoadingPageFragment extends Fragment {
    /**
     * The interaction listener.
     */
    @Nullable
    private InteractionListener mListener;

    /**
     * Creates a new instance.
     */
    public LoadingPageFragment() {
    }

    /**
     * Creates a new instance of the {@link LoadingPageFragment} class.
     *
     * @return The new instance of the {@link LoadingPageFragment} class.
     */
    @NonNull
    public static LoadingPageFragment createInstance() {
        return new LoadingPageFragment();
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);

        if (context instanceof InteractionListener) {
            this.mListener = (InteractionListener)context;
        } else {
            throw new RuntimeException(String.format("%s must implement InteractionListener", context.toString()));
        }
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_page_loading, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button closeButton = view.findViewById(R.id.fragment_page_loading_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InteractionListener listener = LoadingPageFragment.this.mListener;
                if (listener != null) {
                    listener.onClose();
                }
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();

        this.mListener = null;
    }

    /**
     * Activities that include {@link LoadingPageFragment} must implement this interface to handle
     * events.
     */
    public interface InteractionListener {
        /**
         * Called when the user requests that the page be closed.
         */
        void onClose();
    }
}
