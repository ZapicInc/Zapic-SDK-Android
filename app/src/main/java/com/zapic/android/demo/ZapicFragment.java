package com.zapic.android.demo;

import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;


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

    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_zapic, container, false);
        final WebView myWebView = (WebView) view.findViewById(R.id.webview);
        //myWebView.loadUrl("http://www.google.com");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        String url = "https://client.zapic.net";
        //String url = "http://192.168.110.80:3000/";
        myWebView.getSettings().setDomStorageEnabled(true);
        myWebView.getSettings().setSupportZoom(false);
        myWebView.getSettings().setBuiltInZoomControls(false);
        myWebView.getSettings().setSaveFormData(false);
        myWebView.clearCache(true);
        myWebView.getSettings().setJavaScriptEnabled(true);

        final Handler myHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                String code = (String)inputMessage.obj;
                myWebView.evaluateJavascript(code, null);
            }
        };

        myWebView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void dispatch(String json) {
                try {
                    JSONObject action = new JSONObject(json);
                    String actionType = action.getString("type");
                    Log.d("WebView", "Received action " + actionType);

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

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);


        //https://developer.android.com/reference/java/net/HttpURLConnection.html
        URL urlOne = null;
        HttpsURLConnection urlConnection = null;
        String html = "";
        try {
            urlOne = new URL(url);
            urlConnection = (HttpsURLConnection)urlOne.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            if (!urlOne.getHost().equals(urlConnection.getURL().getHost())) {
                // We were hijacked by a captive sign-in portal! Show a connection error with a retry button...
            } else {
                // Read the HTML page into a string value...
                html = getHtml(urlOne.toString());

                Log.d ("html",html);

            }
        } catch (IOException e) {
            // Network or HTTP response error. Show a connection error with a retry button...
        } finally {

            urlConnection.disconnect();
        }

        // Inject global "zapic" object
        int startOfHead = html.indexOf("<head>");
        String updatedHtml = html;
        if (startOfHead == -1){
            // Something went horribly wrong. Show a connection error with a retry button...
        } else {
            String script = "<script>" +
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
            int insertAt = startOfHead + "<head>".length();
            StringBuilder updatedHtmlBuilder = new StringBuilder(html);
            updatedHtmlBuilder.insert(insertAt, script);
            updatedHtml = updatedHtmlBuilder.toString();
        }


// Load WebView with updated HTML
// See notes at https://developer.android.com/reference/android/webkit/WebView.html
       myWebView.loadDataWithBaseURL(url, updatedHtml, "text/html", "utf-8", url);


       // myWebView.loadData(updatedHtml, "text/html", "UTF-8");
        //myWebView.loadData(updatedHtml, "text/html; charset=utf-8", "UTF-8");

        //myWebView.loadUrl(url);
        //myWebView.loadUrl("http://docs.google.com/gview?embedded=true&url=" + url);




        return view;
    }

    public static String getHtml(String url) throws IOException {
        // Build and set timeout values for the request.
        URLConnection connection = (new URL(url)).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.connect();

        // Read and store the result line by line then return the entire string.
        InputStream in = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder html = new StringBuilder();
        for (String line; (line = reader.readLine()) != null; ) {
            html.append(line);
        }
        in.close();

        return html.toString();
    }

}
