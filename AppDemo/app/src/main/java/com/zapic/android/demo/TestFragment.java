package com.zapic.android.demo;

import android.app.Activity;
//import android.app.Fragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
//import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
//import android.support.v4.app.Fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;

/**
 * Created by yashasvi on 11/2/2017.
 */
public class TestFragment extends Fragment {

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View fragment = inflater.inflate(R.layout.fragment_test, container, true);

        return fragment;
    }

    private static final String FRAGMENT_TAG = "zapic.TestFragment";
    public static void launchZapicFragment(Activity parentActivity) {
        TestFragment fragment = (TestFragment)
                parentActivity.getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null) {
            try {
                Log.d("ZAPIC", "Creating fragment");
                fragment = new TestFragment();
                FragmentTransaction trans = parentActivity.getFragmentManager().beginTransaction();
                trans.add(fragment, FRAGMENT_TAG);
                Log.d("ZAPIC", "Creating UnderMaking");
                trans.commit();
            } catch (Throwable th) {
                Log.e("ZAPIC", "Cannot launch Zapic fragment:" + th.getMessage(), th);
            }
        } else {
            Log.d("ZAPIC", "Fragment exists...");
        }
    }

    public static String Hello()
    {
        Log.e("ZAPIC", "Hweelo");
        return  "Hweelo";

    }
}