package com.zapic.sdk.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A utility class that provides static helper methods to read, write, and delete cache files.
 *
 * @author Kyle Dodson
 * @since 1.0.0
 */
final class CacheFileUtilities {
    /**
     * The tag used to identify log entries.
     */
    @NonNull
    private static final String TAG = "CacheFileUtilities";

    /**
     * Deletes a file.
     * <p>
     * This is a potentially long-running, blocking task and should be invoked on a background
     * thread.
     *
     * @param cacheDir          The cache directory.
     * @param fileName          The file name.
     * @param cancellationToken The cancellation token.
     * @throws IOException If repeated errors occur deleting the file.
     */
    @WorkerThread
    static void deleteFile(@NonNull final File cacheDir, @NonNull final String fileName, @NonNull final CancellationToken cancellationToken) throws IOException {
        final File file = new File(cacheDir, fileName);
        int failures = 0;
        while (!file.delete()) {
            if (cancellationToken.isCancelled()) {
                return;
            }

            if (!file.exists()) {
                return;
            }

            if (cancellationToken.isCancelled()) {
                return;
            }

            Log.e(TAG, String.format("Failed to delete file \"%s\" %d time(s)", file.toString(), ++failures));
            if (failures > 10) {
                throw new IOException(String.format("Failed to delete file \"%s\"", file.toString()));
            }
        }
    }

    /**
     * Reads a GZIP compressed, UTF-8 encoded text file and returns the contents as a string.
     * <p>
     * This is a potentially long-running, blocking task and should be invoked on a background
     * thread.
     *
     * @param cacheDir          The cache directory.
     * @param fileName          The file name.
     * @param cancellationToken The cancellation token.
     * @return The contents of the file or {@code null} if the task was cancelled or if the file
     * does not exist.
     * @throws IOException If repeated errors occur reading the file.
     */
    @Nullable
    @WorkerThread
    static String readFile(@NonNull final File cacheDir, @NonNull final String fileName, @NonNull final CancellationToken cancellationToken) throws IOException {
        final File file = new File(cacheDir, fileName);
        int failures = 0;
        while (true) {
            Reader reader = null;
            try {
                final StringBuilder contentBuilder = new StringBuilder();
                reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file)), StandardCharsets.UTF_8));

                if (cancellationToken.isCancelled()) {
                    return null;
                }

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
            } catch (IOException e) {
                Log.e(TAG, String.format("Failed to read file \"%s\" %d time(s)", file.toString(), ++failures), e);
                if (failures > 10) {
                    throw e;
                }
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ignored) {
                    }
                }
            }

            if (cancellationToken.isCancelled()) {
                return null;
            }
        }
    }

    /**
     * Writes a GZIP compressed, UTF-8 encoded text file.
     * <p>
     * This is a potentially long-running, blocking task and should be invoked on a background
     * thread.
     *
     * @param cacheDir          The cache directory.
     * @param fileName          The file name.
     * @param content           The contents of the file.
     * @param cancellationToken The cancellation token.
     * @throws IOException If repeated errors creating or writing the file.
     */
    @WorkerThread
    static void writeFile(@NonNull final File cacheDir, @NonNull final String fileName, @NonNull final String content, @NonNull final CancellationToken cancellationToken) throws IOException {
        final File file = new File(cacheDir, fileName);
        int failures = 0;
        while (true) {
            Writer writer = null;
            try {
                writer = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(file)), StandardCharsets.UTF_8);
                writer.write(content);
                writer.flush();
                return;
            } catch (FileNotFoundException e) {
                throw e;
            } catch (IOException e) {
                Log.e(TAG, String.format("Failed to write file \"%s\" %d time(s)", file.toString(), ++failures), e);
                if (failures > 10) {
                    throw e;
                }
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException ignored) {
                    }
                }
            }

            if (cancellationToken.isCancelled()) {
                return;
            }
        }
    }
}
