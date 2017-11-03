package com.zapic.android.zapiclibrary;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;

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
        final WebView webView = new WebView(activity);

        // TODO: Only enable this in a DEBUG build of the library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setSaveFormData(false);
        webView.getSettings().setSupportZoom(false);

        final Handler myHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                String code = (String)inputMessage.obj;
                webView.evaluateJavascript(code, null);
            }
        };

        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void dispatch(String json) {
                try {
                    JSONObject action = new JSONObject(json);
                    String actionType = action.getString("type");
                    Log.d("ZAPIC-WebView", "Received action of type \"" + actionType + "\"");
                    switch (actionType) {
                        case "LOGIN":
                            Message message1 = myHandler.obtainMessage(1, "window.zapic.dispatch({ type: 'LOGIN_WITH_PLAY_GAME_SERVICES', payload: { serverAuthCode: 'my-server-auth-code' } })");
                            message1.sendToTarget();
                            break;
                        case "APP_STARTED":
                            Message message2 = myHandler.obtainMessage(1, "window.zapic.dispatch({ type: 'OPEN_PAGE', payload: 'default' })");
                            message2.sendToTarget();
                            break;
                        default:
                            break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }, "androidWebView");

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
