package com.zapic.android.zapiclibrary;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;

@SuppressWarnings("unused")
public class ZapicFragment extends Fragment {
    private static final String URL = "http://192.168.110.80:3000/";
    private static final String TAG = "ZAPIC";
    private static final String FRAGMENT_TAG = "com.zapic.androidSdk.ZapicFragment";

    private static WeakReference<ZapicFragment> instanceReference = null;
    private WebView webView = null;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.v(TAG, "ZapicFragment.onActivityCreated()");

        URL url;
        try {
            url = new URL(URL);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Failed to parse URL: " + URL, e);
            return;
        }

        // Start background task.
        new DownloadWebPageAsyncTask(this).execute(url);

        super.onActivityCreated(savedInstanceState);
    }

    public void startWebView(String html) {
        Log.v(TAG, "ZapicFragment.startWebView()");

        Log.d(TAG, "Creating WebView...");
        this.webView = WebViewUtilities.createWebView(getActivity());
        Log.d(TAG, "Created WebView!");

        Log.d(TAG, "Loading HTML in WebView...");
        this.webView.loadDataWithBaseURL(URL, html, "text/html", "utf-8", URL);
        Log.d(TAG, "Loaded HTML in WebView!");
    }

    public static void showZapicFragment(String view) {
        Log.v(TAG, "ZapicFragment.showZapicFragment()");

        ZapicFragment instance = instanceReference.get();
        if (instance != null) {
            instance.webView.evaluateJavascript("window.zapic.dispatch({ type: 'OPEN_PAGE', payload: '" + view + "' })", null);
        }
    }

    public static void startZapicFragment(Activity parentActivity, String version) {
        Log.v(TAG, "ZapicFragment.startZapicFragment()");

        ZapicFragment fragment = (ZapicFragment)parentActivity.getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null) {
            Log.d(TAG, "Creating ZapicFragment");
            try {
                ZapicFragment instance = new ZapicFragment();
                instanceReference = new WeakReference<ZapicFragment>(instance);

                FragmentTransaction trans = parentActivity.getFragmentManager().beginTransaction();
                trans.add(instance, FRAGMENT_TAG);
                trans.commit();
            } catch (Throwable th) {
                Log.e(TAG, "Failed to attach the ZapicFragment", th);
            }
        }
    }

    public static void submitEventZapicFragment(String param) {
        Log.v(TAG, "ZapicFragment.submitEventZapicFragment()");

        ZapicFragment instance = instanceReference.get();
        if (instance != null) {
            instance.webView.evaluateJavascript("window.zapic.dispatch({ type: 'OPEN_PAGE', payload: JSON.parse('" + param + "') })", null);
        }
    }
}
