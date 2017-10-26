package com.zapic.android.demo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;


public class ZapicFragment extends Fragment {
    public ZapicFragment() {
    }

    /**
     * Use this factory method to create a new instance of
     * the Zapic fragment using the provided parameters.
     *
     * @return A new instance of the Zapic fragment.
     */
    public static ZapicFragment newInstance() {
        ZapicFragment fragment = new ZapicFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_zapic, container, false);
        WebView myWebView = (WebView) view.findViewById(R.id.webview);
        myWebView.loadUrl("http://www.google.com");
        return view;
    }
}
