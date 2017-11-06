package com.zapic.android.demo;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

public class LogoutFragment extends Fragment {
    private OnLogoutListener mListener;
    private OnLogoutListener zapicListner;

    public LogoutFragment() {
    }

    public static String Yellow()
    {

        return "Yellow";
    }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnLogoutListener) {
            mListener = (OnLogoutListener)context;
            zapicListner = (OnLogoutListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnLogoutListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragment = inflater.inflate(R.layout.fragment_logout, container, false);

        Button logoutButton = (Button)fragment.findViewById(R.id.button_logout);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                if (id == R.id.button_login) {
                    if (mListener != null)
                        mListener.onLogout();

                }
            }
        });

        Button zapicButton = (Button)fragment.findViewById(R.id.button_open);
        zapicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                if (id == R.id.button_open) {
                    if (mListener != null)
                        mListener.loadZapicWebView();

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

    interface OnLogoutListener {
        void onLogout();
        void loadZapicWebView();
    }
}
