package com.zapic.android.zapiclibrary;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import static android.content.ContentValues.TAG;

public class ZapicActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("ZAPIC", "Started Zapic activity!!!");
    }

    public static void launchZapicActivity(Activity parentActivity) {
        Log.d("ZAPIC", "Launching Zapic activity: parent: " + parentActivity);

        Intent zapicIntent = new Intent(parentActivity, ZapicActivity.class);
        parentActivity.startActivity(zapicIntent);
    }
}
