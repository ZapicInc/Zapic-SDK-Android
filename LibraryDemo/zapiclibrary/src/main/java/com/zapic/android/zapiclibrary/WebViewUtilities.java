package com.zapic.android.zapiclibrary;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.webkit.WebView;
import android.webkit.WebViewClient;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

public class WebViewUtilities {
    @SuppressLint("SetJavaScriptEnabled")
    public static WebView createWebView(Activity activity) {
        WebView webView = new WebView(activity);

        // TODO: Only enable this in a DEBUG build of the library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setSaveFormData(false);
        webView.getSettings().setSupportZoom(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (Uri.parse(url).getScheme().equals("market")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        Activity host = (Activity) view.getContext();
                        host.startActivity(intent);
                        return true;
                    } catch (ActivityNotFoundException e) {
                        return false;
                    }
                }
                return false;
            }
        });

        return webView;
    }

    public static String downloadWebPage(URL url) throws IOException {
        URLConnection connection = null;
        BufferedReader reader = null;
        try {
            connection = url.openConnection();
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.connect();

            StringBuilder html = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            char[] buffer = new char[1024 * 4];
            int n;
            while ((n = reader.read(buffer)) != -1) {
                html.append(buffer, 0, n);
            }

            return html.toString();
        } finally {
            if (reader != null) {
                reader.close();
            }

            if (connection != null) {
                if (HttpsURLConnection.class.isInstance(connection)) {
                    ((HttpsURLConnection) connection).disconnect();
                } else if (HttpURLConnection.class.isInstance(connection)) {
                    ((HttpURLConnection) connection).disconnect();
                }
            }
        }
    }

    public static String injectBridgeIntoWebPage(String html) {
        int startOfHead = html.indexOf("<head>");
        if (startOfHead == -1) {
            // TODO: Consider throwing an exception.
            return html;
        }

        int insertBridgeScriptAt = startOfHead + "<head>".length();
        String bridgeScript = "<script>" +
                "window.zapic = {" +
                "  environment: 'webview'," +
                "  version: 1," +
                "  onLoaded: (action$, publishAction) => {" +
                "    window.zapic.dispatch = (action) => {" +
                "      publishAction(action)" +
                "    };" +
                "    action$.subscribe(action => {" +
                "      window.androidWebView.dispatch(JSON.stringify(action))" +
                "    });" +
                "  }" +
                "}" +
                "</script>";

        StringBuilder bridgeScriptHtml = new StringBuilder(html);
        bridgeScriptHtml.insert(insertBridgeScriptAt, bridgeScript);
        return bridgeScriptHtml.toString();
    }
}
