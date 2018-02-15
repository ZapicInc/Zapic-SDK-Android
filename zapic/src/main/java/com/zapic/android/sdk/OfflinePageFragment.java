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
 * A {@link Fragment} that renders an offline page.
 * <p>
 * Use the {@link OfflinePageFragment#createInstance} factory method to create instances of this
 * fragment.
 * <p>
 * Activities that include this fragment must implement the
 * {@link OfflinePageFragment.InteractionListener} interface.
 *
 * @author Kyle Dodson
 * @since 1.0.0
 */
public final class OfflinePageFragment extends Fragment {
    /**
     * The interaction listener.
     */
    @Nullable
    private InteractionListener mListener;

    /**
     * Creates a new instance.
     */
    public OfflinePageFragment() {
    }

    /**
     * Creates a new instance of the {@link OfflinePageFragment} class.
     *
     * @return The new instance of the {@link OfflinePageFragment} class.
     */
    @NonNull
    public static OfflinePageFragment createInstance() {
        return new OfflinePageFragment();
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
        return inflater.inflate(R.layout.fragment_page_offline, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button closeButton = view.findViewById(R.id.fragment_page_offline_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InteractionListener listener = OfflinePageFragment.this.mListener;
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
     * Activities that include {@link OfflinePageFragment} must implement this interface to handle
     * events.
     */
    public interface InteractionListener {
        /**
         * Called when the user requests that the page be closed.
         */
        void onClose();
    }
}
