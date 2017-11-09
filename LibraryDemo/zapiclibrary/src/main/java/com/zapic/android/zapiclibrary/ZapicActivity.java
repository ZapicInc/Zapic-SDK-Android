package com.zapic.android.zapiclibrary;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageButton;

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
        this.getActionBar().hide();
        setContentView(R.layout.layout_splash);
        finish();
    }


    public void loadSplash()
    {
        this.getActionBar().hide();
        setContentView(R.layout.layout_splash);

        final ImageButton button = (ImageButton) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                finish();
            }
        });

    }

    public void loadPage()
    {
        WebView webView = ZapicFragment.webView;

        if(webView.getParent() != null) {
            ((ViewGroup)webView.getParent()).removeView(webView);
        }
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
