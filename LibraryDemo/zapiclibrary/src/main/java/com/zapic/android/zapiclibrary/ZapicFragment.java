package com.zapic.android.zapiclibrary;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

@SuppressWarnings("unused")
public class ZapicFragment extends Fragment {
    private static final String URL = "https://client.zapic.net";
    private static final String TAG = "ZAPIC";
    private static final String FRAGMENT_TAG = "com.zapic.androidSdk.ZapicFragment";
    private static ArrayDeque<String> storedOfflineEvents = new ArrayDeque<>();

    public static WeakReference<ZapicActivity> activityInstanceReference = null;
    public static WeakReference<ZapicFragment> instanceReference = null;
    public static WebViewBridge webViewBridge = null;
    public static WebView webView = null;




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
        webView = WebViewUtilities.createWebView(getActivity());
        webViewBridge = new WebViewBridge(webView);
        webView.addJavascriptInterface(webViewBridge, "androidWebView");
        Log.d(TAG, "Created WebView!");

        Log.d(TAG, "Loading HTML in WebView...");
        webView.loadDataWithBaseURL(URL, html, "text/html", "utf-8", URL);
        Log.d(TAG, "Loaded HTML in WebView!");
    }

    public static void showZapicFragment(String view) {
        Log.v(TAG, "ZapicFragment.showZapicFragment()");

        ZapicFragment instance = instanceReference.get();
        if (instance != null) {
            // TODO: Show activity with loading fragment.
            Intent myIntent = new Intent(instance.getActivity(), ZapicActivity.class);
            instance.getActivity().startActivity(myIntent);

            webViewBridge.dispatchMessage("{ type: 'OPEN_PAGE', payload: '" + view + "' }");
        }
    }


    public static void startZapicFragment(Activity parentActivity, String version) {
        Log.v(TAG, "ZapicFragment.startZapicFragment()");

        ZapicFragment fragment = (ZapicFragment)parentActivity.getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null) {
            Log.d(TAG, "Creating ZapicFragment");
            try {
                ZapicFragment instance = new ZapicFragment();
                instanceReference = new WeakReference<>(instance);

                FragmentTransaction trans = parentActivity.getFragmentManager().beginTransaction();
                trans.add(instance, FRAGMENT_TAG);
                trans.commit();
            } catch (Throwable th) {
                Log.e(TAG, "Failed to attach the ZapicFragment", th);
            }
        }
    }

    public static void submitEventZapicFragment(String param) {
        Log.v("Zapic", "ZapicFragment.submitEventZapicFragment()");

        ZapicFragment instance = instanceReference.get();
        if (instance != null && webViewBridge.getPageReady()) {
            if(!storedOfflineEvents.isEmpty())
            {
                Log.v("Zapic", "Empty Queue.");
                String[] arr =  new String[storedOfflineEvents.size()];
                for(int i = 0; i < storedOfflineEvents.size() ; i++)
                {
                    arr[i] = storedOfflineEvents.getFirst();
                    storedOfflineEvents.removeFirst();
                }//(String[]) storedOfflineEvents.toArray();
                webViewBridge.dispatchMessages(arr);
            }

           webViewBridge.dispatchMessage("{ type: 'SUBMIT_EVENT', payload: JSON.parse('" + param + "') }");
            Log.v("Zapic", "Passed Message.");
        }else if(!webViewBridge.getPageReady())
        {
            Log.v("Zapic", "Passed Saved in Queue.");
            storedOfflineEvents.add(param);

        }
    }

}
