package com.zapic.android.zapiclibrary;

import android.os.AsyncTask;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.net.URL;

public class DownloadWebPageAsyncTask extends AsyncTask<URL, Void, String> {
    private final WeakReference<ZapicFragment> fragmentReference;

    public DownloadWebPageAsyncTask(ZapicFragment fragment) {
        Log.v("ZAPIC", "new DownloadWebPageAsyncTask()");
        this.fragmentReference = new WeakReference<>(fragment);
    }

    @Override
    protected String doInBackground(URL... url) {

        Log.v("ZAPIC", "DownloadWebPageAsyncTask.doInBackground()");

        Log.d("ZAPIC", "Downloading HTML...");
        String html = null;
        try {
            html = WebViewUtilities.injectBridgeIntoWebPage(WebViewUtilities.downloadWebPage(url[0]));
            Log.d("ZAPIC", "Downloaded HTML!");
            Log.d("ZAPIC", html);
        } catch (Exception e) {
            Log.e("ZAPIC", "Failed to download HTML", e);
        }

        return html;
    }

    @Override
    protected void onPostExecute(String html) {
        Log.v("ZAPIC", "DownloadWebPageAsyncTask.onPostExecute()");

        ZapicFragment fragment = this.fragmentReference.get();
        if (fragment != null) {
            fragment.startWebView(html);
        }
    }
}
