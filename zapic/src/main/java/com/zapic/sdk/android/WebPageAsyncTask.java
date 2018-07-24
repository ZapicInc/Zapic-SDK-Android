package com.zapic.sdk.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.AnyThread;
import android.support.annotation.CheckResult;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.webkit.ValueCallback;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

/**
 * An asynchronous task that downloads and caches the Zapic web page.
 *
 * @author Kyle Dodson
 * @since 1.2.0
 */
final class WebPageAsyncTask extends AsyncTask<Void, Integer, WebPage> implements CancellationToken {
    /**
     * The number of failed retries before a stale Zapic web page is returned.
     */
    private static final int STALE_THRESHOLD = 2;

    /**
     * The tag used to identify log messages.
     */
    @NonNull
    private static final String TAG = "WebPageAsyncTask";

    /**
     * The HTTPS connection and input stream read timeout (in milliseconds).
     */
    private static final int TIMEOUT = 10000;

    /**
     * The URL of the Zapic web page with a trailing slash.
     */
    @NonNull
    private static final String URL_WITH_SLASH = "https://app.zapic.net/";

    /**
     * The global application context.
     */
    @NonNull
    @SuppressLint("StaticFieldLeak")
    private final Context mApplicationContext;

    /**
     * The callback invoked after an attempt to download the Zapic web page fails. This may be
     * invoked multiple times.
     */
    @NonNull
    private final ValueCallback<Integer> mFailureCallback;

    /**
     * The cache file manager.
     */
    @NonNull
    private final FileManager mFileManager;

    /**
     * The callback invoked after an attempt to download the Zapic web page succeeds. This will be
     * invoked at most one time.
     */
    @NonNull
    private final ValueCallback<WebPage> mSuccessCallback;

    /**
     * Creates a new {@link WebPageAsyncTask} instance.
     *
     * @param context         Any context object (e.g. the global {@link android.app.Application} or
     *                        an {@link android.app.Activity}).
     * @param successCallback The callback invoked after an attempt to download the Zapic web page
     *                        succeeds. This will be invoked at most one time.
     * @param failureCallback The callback invoked after an attempt to download the Zapic web page
     *                        fails. This may be invoked multiple times.
     */
    @AnyThread
    WebPageAsyncTask(@NonNull final Context context, @NonNull final ValueCallback<WebPage> successCallback, @NonNull final ValueCallback<Integer> failureCallback) {
        mApplicationContext = context.getApplicationContext();
        mFailureCallback = failureCallback;
        mFileManager = new FileManager(mApplicationContext);
        mSuccessCallback = successCallback;
    }

    /**
     * Caches the specified Zapic web page.
     * <p>
     * This returns early if the task is cancelled.
     *
     * @param webPage The Zapic web page.
     */
    @WorkerThread
    private void cacheWebPage(@NonNull final WebPage webPage) {
        mFileManager.putWebPage(webPage, this);
    }

    @Nullable
    @Override
    @WorkerThread
    protected WebPage doInBackground(final Void... voids) {
        mFileManager.deleteShareDir(this);
        if (isCancelled()) {
            return null;
        }

        final WebPage cachedWebPage = getCachedWebPage();
        if (isCancelled()) {
            return null;
        }

        boolean fromCache;
        WebPage webPage;
        if (cachedWebPage == null || isCachedWebPageStale(cachedWebPage)) {
            webPage = downloadWebPage(cachedWebPage);
            if (isCancelled()) {
                return null;
            }

            assert webPage != null : "webPage == null";
            if (webPage == cachedWebPage) {
                fromCache = true;
            } else {
                fromCache = false;
                cacheWebPage(webPage);
                if (isCancelled()) {
                    return null;
                }
            }
        } else {
            fromCache = true;
            webPage = cachedWebPage;
        }

        webPage = injectScript(webPage);
        if (fromCache) {
            Log.i(TAG, "Loading cached Zapic web page");
        } else {
            Log.i(TAG, "Loading downloaded Zapic web page");
        }

        return webPage;
    }

    /**
     * Downloads the Zapic web page.
     * <p>
     * The specified cached Zapic web page is returned if it is not {@code null} and downloading the
     * Zapic web page fails repeatedly.
     * <p>
     * This returns {@code null} if the task is cancelled.
     *
     * @param cachedWebPage The cached Zapic web page.
     * @return The Zapic web page or {@code null} if the task was cancelled.
     */
    @CheckResult
    @Nullable
    @WorkerThread
    private WebPage downloadWebPage(@Nullable final WebPage cachedWebPage) {
        final URL url;
        try {
            url = new URL(URL_WITH_SLASH);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        Log.i(TAG, String.format("Downloading Zapic web page from %s", url));
        final Random random = new Random();
        int failures = 0;
        while (true) {
            final WebPage webPage = downloadWebPage2(url);
            if (webPage != null || isCancelled()) {
                return webPage;
            }

            // Ensure we don't overflow failures.
            if (Integer.MAX_VALUE != failures) {
                ++failures;
            }

            // Return a stale web page if available.
            if (failures > STALE_THRESHOLD && cachedWebPage != null) {
                return cachedWebPage;
            }

            publishProgress(failures);
            try {
                // Ensure we don't overflow maximumDelay; limit the maximum delay to ~30 minutes.
                final int clampedRetries = (failures > 14 ? 14 : failures) - 1;
                final int maximumDelay = (int) Math.pow(2, clampedRetries) * 100;

                // Limit the minimum delay to 100 milliseconds.
                final int delay = random.nextInt(maximumDelay) + 100;
                Thread.sleep(delay);
            } catch (InterruptedException ignored) {
            }

            if (isCancelled()) {
                return null;
            }
        }
    }

    /**
     * Downloads the Zapic web page.
     * <p>
     * This returns {@code null} if an error occurs downloading the Zapic web page or if the task is
     * cancelled.
     *
     * @param url The URL of the Zapic web page.
     * @return The Zapic web page or {@code null} if an error occurs downloading the Zapic web page
     * or if the task is cancelled..
     */
    @CheckResult
    @Nullable
    @WorkerThread
    private WebPage downloadWebPage2(@NonNull final URL url) {
        HttpsURLConnection connection = null;
        BufferedReader reader = null;
        try {
            // Fetch web page from web server.
            connection = (HttpsURLConnection) url.openConnection();
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.connect();
            if (isCancelled()) {
                return null;
            }

            // Parse response headers.
            final int statusCode = connection.getResponseCode();
            if (statusCode == HttpsURLConnection.HTTP_OK) {
                final Map<String, List<String>> fields = connection.getHeaderFields();
                final HashMap<String, String> headers = new HashMap<>();
                for (Entry<String, List<String>> field : fields.entrySet()) {
                    final String key = field.getKey();
                    final List<String> value = field.getValue();
                    if (key != null && value != null && value.size() > 0) {
                        headers.put(key, value.get(0));
                    }
                }

                // Parse response body.
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                if (isCancelled()) {
                    return null;
                }

                final StringBuilder htmlBuilder = new StringBuilder();
                final char[] buffer = new char[1024 * 4];
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    if (isCancelled()) {
                        return null;
                    }

                    htmlBuilder.append(buffer, 0, n);
                }

                Log.i(TAG, "Downloaded Zapic web page");
                return new WebPage(htmlBuilder.toString(), headers, System.currentTimeMillis());
            }

            Log.e(TAG, String.format("Downloading Zapic web page failed with HTTP status code %d", statusCode));
        } catch (IOException e) {
            Log.e(TAG, "Downloading Zapic web page failed", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }

            if (connection != null) {
                connection.disconnect();
            }
        }

        return null;
    }

    /**
     * Gets the cached Zapic web page.
     * <p>
     * This returns {@code null} if the task is cancelled.
     *
     * @return The cached Zapic web page or {@code null} if it does not exist or if the task is
     * cancelled.
     */
    @CheckResult
    @Nullable
    @WorkerThread
    private WebPage getCachedWebPage() {
        return mFileManager.getWebPage(this);
    }

    /**
     * Injects custom JavaScript to initialize the Zapic web page's WebView platform.
     *
     * @param webPage The original Zapic web page.
     * @return The modified Zapic web page.
     */
    @CheckResult
    @NonNull
    @WorkerThread
    private WebPage injectScript(@NonNull final WebPage webPage) {
        final String html = webPage.getHtml();
        final int startOfHead = html.indexOf("<head>");
        if (startOfHead == -1) {
            return webPage;
        }

        final int endOfHead = startOfHead + "<head>".length();
        final String script = "<script>" +
                "window.androidWebViewWatchdog = window.setTimeout(function () {" +
                "  window.androidWebView.dispatch('{\"type\":\"APP_FAILED\"}');" +
                "}, " + Integer.toString(TIMEOUT, 10) + ");" +
                "window.zapic = {" +
                "  environment: 'webview'," +
                "  version: 3," +
                "  onLoaded: function (action$, publishAction) {" +
                "    window.clearTimeout(window.androidWebViewWatchdog);" +
                "    delete window.androidWebViewWatchdog;" +
                "    window.zapic.dispatch = function (action) {" +
                "      publishAction(action)" +
                "    };" +
                "    action$.subscribe(function (action) {" +
                "      window.androidWebView.dispatch(JSON.stringify(action))" +
                "    });" +
                "  }," +
                "  packageName: '" + mApplicationContext.getPackageName().replace("'", "\\'") + "', " +
                "  androidVersion: '" + String.valueOf(Build.VERSION.SDK_INT).replace("'", "\\'") + "'," +
                "  sdkVersion: '" + BuildConfig.VERSION_NAME.replace("'", "\\'") + "'," +
                "};" +
                "</script>".replaceAll(" +", " ");

        final StringBuilder htmlBuilder = new StringBuilder(html);
        htmlBuilder.insert(endOfHead, script);

        return new WebPage(htmlBuilder.toString(), webPage.getHeaders(), webPage.getLastValidated());
    }

    /**
     * Determines whether the specified cached Zapic web page is stale.
     *
     * @param cachedWebPage The cached Zapic web page.
     * @return {@code true} if the cached Zapic web page is stale; otherwise, {@code false}.
     */
    @CheckResult
    @WorkerThread
    private boolean isCachedWebPageStale(@NonNull final WebPage cachedWebPage) {
        final long lastValidated = cachedWebPage.getLastValidated();
        if (lastValidated > 0) {
            final Map<String, String> headers = cachedWebPage.getHeaders();
            long maxAge = 0;
            for (Entry<String, String> header : headers.entrySet()) {
                final String key = header.getKey();
                if (key == null || !"cache-control".equalsIgnoreCase(key)) {
                    continue;
                }

                final String value = header.getValue();
                if (value == null) {
                    continue;
                }

                final Pattern pattern = Pattern.compile("max-age=(\\d+)", Pattern.CASE_INSENSITIVE);
                final Matcher matcher = pattern.matcher(value);
                if (matcher.find()) {
                    try {
                        maxAge = Long.parseLong(matcher.group(1), 10) * 1000;
                        break;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            final long staleness = Math.abs(System.currentTimeMillis() - lastValidated);
            return staleness > maxAge;
        }

        return true;
    }

    @MainThread
    @Override
    protected void onPostExecute(@Nullable final WebPage webPage) {
        if (webPage != null) {
            mSuccessCallback.onReceiveValue(webPage);
        }
    }

    @MainThread
    @Override
    protected void onProgressUpdate(@Nullable final Integer... values) {
        if (values != null && values.length > 0 && values[0] != null) {
            mFailureCallback.onReceiveValue(values[0]);
        }
    }
}
