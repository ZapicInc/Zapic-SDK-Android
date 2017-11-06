package com.zapic.android.zapiclibrary;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import java.lang.ref.WeakReference;

import static android.content.ContentValues.TAG;

public class ZapicActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ZapicFragment.activityInstanceReference = new WeakReference<>(this);
        super.onCreate(savedInstanceState);
        Log.d("ZAPIC", "Started Zapic activity!!!");
        loadSplash();
    }

    public void closePage()
    {
        setContentView(R.layout.layout_splash);
        finish();
    }

    public void loadSplash()
    {
        setContentView(R.layout.layout_splash);
    }

    public void loadPage()
    {
        WebView webView = ZapicFragment.webView;
        if (webView != null) {
            setContentView(webView);
        }
    }

    public static void launchZapicActivity(Activity parentActivity) {
        Log.d("ZAPIC", "Launching Zapic activity: parent: " + parentActivity);

        Intent zapicIntent = new Intent(parentActivity, ZapicActivity.class);
        parentActivity.startActivity(zapicIntent);
    }
}
