package com.zapic.sdk.android;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Provides utility methods to manage files in the cache directory.
 *
 * @author Kyle Dodson
 * @since 1.2.0
 */
final class FileManager {
    /**
     * The events file name.
     */
    @NonNull
    private static final String EVENTS_FILE_NAME = "events.json.gz";

    /**
     * The installation ID file name.
     */
    @NonNull
    private static final String INSTALLATION_ID_NAME = "id.json.gz";

    /**
     * The tag used to identify log messages.
     */
    @NonNull
    private static final String TAG = "CacheManager";

    /**
     * The web page file name.
     */
    @NonNull
    private static final String WEB_PAGE_FILE_NAME = "page.json.gz";

    /**
     * The cache directory.
     */
    @NonNull
    private final String mCacheDir;

    /**
     * The files directory.
     */
    @NonNull
    private final String mFilesDir;

    /**
     * The share directory.
     */
    @NonNull
    private final String mShareDir;

    /**
     * Creates a new {@link FileManager} instance.
     *
     * @param context Any context object (e.g. the global {@link Application} or an
     *                {@link Activity}).
     */
    @AnyThread
    FileManager(@NonNull final Context context) {
        mCacheDir = new File(context.getCacheDir(), "Zapic").getAbsolutePath();
        mFilesDir = new File(context.getFilesDir(), "Zapic").getAbsolutePath();
        mShareDir = new File(mCacheDir, "Share").getAbsolutePath();
    }

    /**
     * Recursively deletes files and directories.
     * <p>
     * This is a potentially long-running, blocking task and must be invoked on a background thread.
     *
     * @param fileOrDirectory   The file or directory.
     * @param cancellationToken The cancellation token.
     * @return {@code true} if the file or directory was deleted; {@code false} if the file or
     * directory was not deleted.
     */
    @WorkerThread
    private static boolean deleteRecursively(@NonNull final File fileOrDirectory, @NonNull final CancellationToken cancellationToken) {
        if (cancellationToken.isCancelled()) {
            return false;
        }

        if (fileOrDirectory.isDirectory()) {
            for (File file : fileOrDirectory.listFiles()) {
                if (!deleteRecursively(file, cancellationToken)) {
                    return false;
                }
            }
        }

        return fileOrDirectory.delete();
    }

    /**
     * Reads a GZIP compressed, UTF-8 encoded text file.
     * <p>
     * This is a potentially long-running, blocking task and must be invoked on a background thread.
     *
     * @param fileName          The absolute file path and name.
     * @param cancellationToken The cancellation token.
     * @return The file contents.
     * @throws IOException If an error occurs creating directories or writing the file.
     */
    @Nullable
    @WorkerThread
    private static String readCompressedTextFile(@NonNull final String fileName, @NonNull final CancellationToken cancellationToken) throws IOException {
        final File file = new File(fileName);
        Reader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file)), StandardCharsets.UTF_8));
            if (cancellationToken.isCancelled()) {
                return null;
            }

            final StringBuilder contentBuilder = new StringBuilder();
            final char[] buffer = new char[1024 * 4];
            int n;
            while ((n = reader.read(buffer)) != -1) {
                if (cancellationToken.isCancelled()) {
                    return null;
                }

                contentBuilder.append(buffer, 0, n);
            }

            return contentBuilder.toString();
        } catch (FileNotFoundException e) {
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Writes a GZIP compressed, UTF-8 encoded text file.
     * <p>
     * This is a potentially long-running, blocking task and must be invoked on a background thread.
     *
     * @param fileName          The absolute file path and name.
     * @param content           The file contents.
     * @param cancellationToken The cancellation token.
     * @throws IOException If an error occurs creating directories or writing the file.
     */
    @WorkerThread
    private static void writeCompressedTextFile(@NonNull final String fileName, @NonNull final String content, @NonNull final CancellationToken cancellationToken) throws IOException {
        final File file = new File(fileName);
        final File directory = file.getParentFile();
        if (!directory.exists() && !directory.mkdirs() && !directory.exists()) {
            throw new IOException("The directories could not be created");
        }

        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(file)), StandardCharsets.UTF_8);
            if (cancellationToken.isCancelled()) {
                return;
            }

            writer.write(content);
            writer.flush();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Deletes the backup of Zapic events from the cache.
     * <p>
     * This is a potentially long-running, blocking task and must be invoked on a background thread.
     *
     * @return {@code true} if the backup of Zapic events was deleted; {@code false} if the backup
     * of Zapic events was not deleted.
     */
    @WorkerThread
    boolean deleteEvents() {
        return new File(mCacheDir, EVENTS_FILE_NAME).delete();
    }

    /**
     * Deletes the share directory.
     * <p>
     * This is a potentially long-running, blocking task and must be invoked on a background thread.
     *
     * @param cancellationToken The cancellation token.
     * @return {@code true} if the share directory was deleted; {@code false} if the share directory
     * was not deleted.
     */
    @WorkerThread
    boolean deleteShareDir(@NonNull final CancellationToken cancellationToken) {
        final File shareDir = new File(mShareDir);
        return !shareDir.exists() || deleteRecursively(shareDir, cancellationToken);
    }

    /**
     * Gets the backup of Zapic events from the cache.
     * <p>
     * This is a potentially long-running, blocking task and must be invoked on a background thread.
     *
     * @param cancellationToken The cancellation token.
     * @return The backup of Zapic events or {@code null} if none exist.
     */
    @Nullable
    @WorkerThread
    JSONObject[] getEvents(@NonNull final CancellationToken cancellationToken) {
        for (int i = 0; i < 3; i++) {
            try {
                final String content = readCompressedTextFile(new File(mCacheDir, EVENTS_FILE_NAME).getAbsolutePath(), cancellationToken);
                if (content == null) {
                    return null;
                }

                final JSONArray json = new JSONArray(content);
                ArrayList<JSONObject> events = new ArrayList<>();
                for (int j = 0; j < json.length(); j++) {
                    events.add(json.getJSONObject(j));
                }

                return events.toArray(new JSONObject[events.size()]);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse cached Zapic web page", e);
                return null;
            } catch (IOException e) {
                Log.e(TAG, "Failed to read Zapic web page from cache", e);
            }
        }

        return null;
    }

    /**
     * Gets the installation ID from the disk.
     * <p>
     * This is a potentially long-running, blocking task and must be invoked on a background thread.
     *
     * @param cancellationToken The cancellation token.
     * @return The installation ID or {@code null} if it does not exist.
     */
    @Nullable
    @WorkerThread
    UUID getInstallationId(@NonNull final CancellationToken cancellationToken) {
        for (int i = 0; i < 3; i++) {
            try {
                final String content = readCompressedTextFile(new File(mFilesDir, INSTALLATION_ID_NAME).getAbsolutePath(), cancellationToken);
                if (content == null) {
                    return null;
                }

                final JSONObject json = new JSONObject(content);
                final String id = json.getString("id");
                return UUID.fromString(id);
            } catch (IllegalArgumentException | JSONException e) {
                Log.e(TAG, "Failed to parse installation ID", e);
                return null;
            } catch (IOException e) {
                Log.e(TAG, "Failed to read installation ID", e);
            }
        }

        return null;
    }

    /**
     * Gets the Zapic web page from the cache.
     * <p>
     * This is a potentially long-running, blocking task and must be invoked on a background thread.
     *
     * @param cancellationToken The cancellation token.
     * @return The Zapic web page or {@code null} if it does not exist.
     */
    @Nullable
    @WorkerThread
    WebPage getWebPage(@NonNull final CancellationToken cancellationToken) {
        for (int i = 0; i < 3; i++) {
            try {
                final String content = readCompressedTextFile(new File(mCacheDir, WEB_PAGE_FILE_NAME).getAbsolutePath(), cancellationToken);
                if (content == null) {
                    return null;
                }

                final JSONObject json = new JSONObject(content);
                final JSONObject fields = json.getJSONObject("headers");
                final String html = json.getString("html");
                final long lastValidated = json.getLong("lastValidated");

                final Iterator<String> fieldsIterator = fields.keys();
                final HashMap<String, String> headers = new HashMap<>();
                while (fieldsIterator.hasNext()) {
                    final String field = fieldsIterator.next();
                    headers.put(field.toLowerCase(), fields.getString(field));
                }

                return new WebPage(html, headers, lastValidated);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse cached Zapic web page", e);
                return null;
            } catch (IOException e) {
                Log.e(TAG, "Failed to read Zapic web page from cache", e);
            }
        }

        return null;
    }

    /**
     * Puts the specified backup of Zapic events in the cache.
     * <p>
     * This is a potentially long-running, blocking task and must be invoked on a background thread.
     *
     * @param events            The backup of Zapic events.
     * @param cancellationToken The cancellation token.
     */
    @WorkerThread
    boolean putEvents(@NonNull final JSONObject[] events, @NonNull final CancellationToken cancellationToken) {
        final JSONArray array = new JSONArray();
        for (JSONObject event : events) {
            array.put(event);
        }

        final String content = array.toString();

        for (int i = 0; i < 3; i++) {
            try {
                writeCompressedTextFile(new File(mCacheDir, EVENTS_FILE_NAME).getAbsolutePath(), content, cancellationToken);
                return !cancellationToken.isCancelled();
            } catch (IOException e) {
                Log.e(TAG, "Failed to write Zapic events to cache", e);
            }
        }

        return false;
    }

    /**
     * Puts the specified installation ID in the disk.
     * <p>
     * This is a potentially long-running, blocking task and must be invoked on a background thread.
     *
     * @param installationId    The installation ID.
     * @param cancellationToken The cancellation token.
     * @return {@code true} if the installation ID was saved; {@code false} if the installation ID
     * was not saved.
     */
    @WorkerThread
    boolean putInstallationId(@NonNull final UUID installationId, @NonNull final CancellationToken cancellationToken) {
        final String content;
        try {
            final JSONObject json = new JSONObject()
                    .put("id", installationId.toString());

            content = json.toString();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to encode installation ID", e);
            return false;
        }

        for (int i = 0; i < 3; i++) {
            try {
                writeCompressedTextFile(new File(mFilesDir, INSTALLATION_ID_NAME).getAbsolutePath(), content, cancellationToken);
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Failed to write installation ID to disk", e);
            }
        }

        return  false;
    }

    /**
     * Puts the specified Zapic web page in the cache.
     * <p>
     * This is a potentially long-running, blocking task and must be invoked on a background thread.
     *
     * @param webPage           The Zapic web page.
     * @param cancellationToken The cancellation token.
     */
    @WorkerThread
    void putWebPage(@NonNull final WebPage webPage, @NonNull final CancellationToken cancellationToken) {
        final String content;
        try {
            final JSONObject headers = new JSONObject();
            for (Entry<String, String> header : webPage.getHeaders().entrySet()) {
                headers.put(header.getKey(), header.getValue());
            }

            final JSONObject json = new JSONObject()
                    .put("headers", headers)
                    .put("html", webPage.getHtml())
                    .put("lastValidated", webPage.getLastValidated());

            content = json.toString();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to encode Zapic web page", e);
            return;
        }

        for (int i = 0; i < 3; i++) {
            try {
                writeCompressedTextFile(new File(mCacheDir, WEB_PAGE_FILE_NAME).getAbsolutePath(), content, cancellationToken);
                return;
            } catch (IOException e) {
                Log.e(TAG, "Failed to write Zapic web page to cache", e);
            }
        }
    }
}
