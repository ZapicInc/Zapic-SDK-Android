package com.zapic.android.sdk;

import android.os.Build;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.webkit.WebView;

final class WebViewManager {
    @NonNull
    private WebView mWebView;

    @MainThread
    void onClosed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.mWebView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_WAIVED, false);
        }
    }

    @MainThread
    void onOpen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.mWebView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.mWebView.getSettings().setOffscreenPreRaster(true);
        }
    }

    @MainThread
    void onOpened() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.mWebView.getSettings().setOffscreenPreRaster(false);
        }
    }
}
