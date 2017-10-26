package com.zapic.android.demo;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class LoginFragment extends Fragment {
    private OnLoginListener mListener;

    public LoginFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnLoginListener) {
            mListener = (OnLoginListener)context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnLoginListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragment = inflater.inflate(R.layout.fragment_login, container, false);

        Button loginButton = (Button)fragment.findViewById(R.id.button_login);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.button_login:
                        if (mListener != null)
                            mListener.onLogin();
                }
            }
        });

        return fragment;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    interface OnLoginListener {
        void onLogin();
    }
}
