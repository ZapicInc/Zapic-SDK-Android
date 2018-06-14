package com.zapic.sdk.android;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.MutableContextWrapper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Process;
import android.provider.MediaStore;
import android.support.annotation.AnyThread;
import android.support.annotation.CheckResult;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.FileProvider;
import android.transition.Fade;
import android.util.Log;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.Window;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * An activity used to show the Zapic user interface.
 *
 * @author Kyle Dodson
 * @since 1.0.0
 */
public final class ZapicActivity extends Activity {
    /**
     * Identifies a permission request to capture an image from the device camera.
     */
    private static final int CAMERA_PERMISSION_REQUEST = 1000;

    /**
     * The duration of the activity fade-in and fade-out animations. This was set to Android's
     * current default value for the resource {@code config_activityDefaultDur}.
     */
    private static final int FADE_DURATION = 220;

    /**
     * Identifies an action request to capture an image from the device camera or pick an image from
     * the media library.
     */
    private static final int IMAGE_REQUEST = 1001;

    /**
     * The tag used to identify log messages.
     */
    @NonNull
    private static final String TAG = "ZapicActivity";

    /**
     * The current animation.
     */
    @Nullable
    private ViewPropertyAnimator mAnimation;

    /**
     * A value indicating whether the activity has started.
     */
    private boolean mStarted;

    /**
     * The {@link WebView} instance.
     */
    @Nullable
    private WebView mWebView;

    /**
     * The {@link ViewManager} instance.
     */
    @Nullable
    private ViewManager mViewManager;

    /**
     * The image chooser callback.
     */
    @Nullable
    private ValueCallback<Uri[]> mImageChooserCallback;

    /**
     * The temporarily shared image URI for camera capture.
     */
    @Nullable
    private Uri mImageUriForCamera;

    /**
     * Creates a new {@link ZapicActivity} instance.
     */
    @MainThread
    public ZapicActivity() {
        mAnimation = null;
        mImageChooserCallback = null;
        mImageUriForCamera = null;
        mStarted = false;
        mViewManager = null;
        mWebView = null;
    }

    /**
     * Creates an {@link Intent} that starts a {@link ZapicActivity} and shows the default page.
     *
     * @param activity The parent {@link Activity} instance.
     * @return The {@link Intent}.
     * @throws IllegalArgumentException If {@code activity} is {@code null}.
     */
    @AnyThread
    @CheckResult
    @NonNull
    @SuppressWarnings({"unused", "WeakerAccess"})
    public static Intent createIntent(@Nullable final Activity activity) {
        return ZapicActivity.createIntent(activity, "default");
    }

    /**
     * Creates an {@link Intent} that starts a {@link ZapicActivity} and shows the specified page.
     *
     * @param activity The parent {@link Activity} instance.
     * @param page     The page to show.
     * @return The {@link Intent}.
     * @throws IllegalArgumentException If {@code activity} or {@code page} are {@code null}.
     */
    @AnyThread
    @CheckResult
    @NonNull
    @SuppressWarnings({"unused", "WeakerAccess"})
    public static Intent createIntent(@Nullable final Activity activity, @Nullable final String page) {
        if (activity == null) {
            throw new IllegalArgumentException("activity must not be null");
        }

        if (page == null) {
            throw new IllegalArgumentException("page must not be null");
        }

        final Intent intent = new Intent(activity, ZapicActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("PAGE", page);
        return intent;
    }

    /**
     * Enables an immersive full-screen mode. This hides the system status and navigation bars until
     * the user swipes in from the edges of the screen.
     */
    @MainThread
    private void enableImmersiveFullScreenMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LOW_PROFILE);
    }

    /**
     * Gets a value indicating whether the camera permission is explicitly declared in the manifest.
     * <p>
     * From the {@code ACTION_IMAGE_CAPTURE} documentation:
     * </p>
     * <blockquote>
     * Note: if you app targets {@code M} and above and declares as using the
     * {@code Manifest.permission.CAMERA} permission which is not granted, then attempting to
     * use this action will result in a {@code SecurityException}.
     * </blockquote>
     *
     * @return {@code true} if the camera permission is explicitly declared in the manifest;
     * otherwise, {@code false}.
     * @see <a href="https://developer.android.com/reference/android/provider/MediaStore#ACTION_IMAGE_CAPTURE">ACTION_IMAGE_CAPTURE</a>
     */
    @CheckResult
    @MainThread
    private boolean hasCameraPermissionInManifest() {
        final PackageManager packageManager = getPackageManager();
        final PackageInfo packageInfo;
        try {
            packageInfo = packageManager.getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
        } catch (NameNotFoundException e) {
            return false;
        }

        final String[] permissions = packageInfo.requestedPermissions;
        if (permissions != null) {
            for (String permission : permissions) {
                if (permission.equals(Manifest.permission.CAMERA)) {
                    return true;
                }
            }
        }

        return false;
    }

    @MainThread
    private void hideLoadingPage() {
        // Hide progress bar.
        final ProgressBar progressBar = findViewById(R.id.activity_zapic_progress_bar);
        progressBar.setVisibility(View.GONE);
    }

    @MainThread
    private void hideRetryPage() {
        // Hide close button.
        final ImageButton closeButton = findViewById(R.id.activity_zapic_close);
        closeButton.setVisibility(View.GONE);

        // Hide retry button.
        final RelativeLayout warningPanel = findViewById(R.id.activity_zapic_retry_container);
        warningPanel.setVisibility(View.GONE);
    }

    @MainThread
    private void hideWebPage() {
        // Adjust background.
        final FrameLayout layout = findViewById(R.id.activity_zapic_container);
        layout.setBackgroundColor(Color.argb(204, 0, 0, 0));

        // Cancel animation.
        if (mAnimation != null) {
            mAnimation.cancel();
            mAnimation = null;
        }

        if (mWebView != null) {
            mWebView.setVisibility(View.GONE);
            mWebView.setY(0);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                final WebSettings settings = mWebView.getSettings();
                settings.setOffscreenPreRaster(false);
            }

            layout.removeView(mWebView);

            final MutableContextWrapper webViewContext = (MutableContextWrapper) mWebView.getContext();
            webViewContext.setBaseContext(getApplicationContext());

            mWebView = null;
        }
    }

    @MainThread
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onActivityResult");
        }

        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case IMAGE_REQUEST:
                Uri imageUri;
                if (resultCode == RESULT_OK) {
                    final boolean isCamera;
                    if (data == null || (data.getData() == null && data.getClipData() == null)) {
                        isCamera = true;
                    } else {
                        final String action = data.getAction();
                        isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
                    }

                    if (isCamera) {
                        imageUri = mImageUriForCamera;
                    } else {
                        imageUri = data.getData();
                    }
                } else {
                    imageUri = null;
                }

                if (mImageChooserCallback != null) {
                    mImageChooserCallback.onReceiveValue(imageUri == null ? null : new Uri[]{imageUri});
                    mImageChooserCallback = null;
                }

                mImageUriForCamera = null;
            default:
                break;
        }
    }

    @MainThread
    public void onCloseClick(@Nullable final View view) {
        final ImageButton closeButton = findViewById(R.id.activity_zapic_close);
        if (closeButton == null || !closeButton.equals(view)) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition();
        } else {
            finish();
        }
    }

    @MainThread
    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreate");
        }

        final Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final Fade fade = new Fade();
            fade.setDuration(FADE_DURATION);
            window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
            window.setEnterTransition(fade);
            window.setReturnTransition(fade);
            window.setAllowEnterTransitionOverlap(false);
        }

        enableImmersiveFullScreenMode();
        window.getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    enableImmersiveFullScreenMode();
                }
            }
        });

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zapic);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            final ProgressBar progressBar = findViewById(R.id.activity_zapic_progress_bar);
            progressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_IN);
        }

        showLoadingPage();
        Zapic.attachFragment(this);

        mViewManager = Zapic.onAttachedFragment(this);
        mViewManager.onActivityCreated(this);
    }

    @MainThread
    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onDestroy");
        }

        hideWebPage();

        if (mImageChooserCallback != null) {
            mImageChooserCallback.onReceiveValue(null);
            mImageChooserCallback = null;
        }

        mImageUriForCamera = null;

        assert mViewManager != null : "mViewManager == null";
        mViewManager.onActivityDestroyed(this);

        Zapic.detachFragment(this);
        super.onDestroy();
    }

    @MainThread
    @Override
    protected void onNewIntent(@NonNull final Intent intent) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onNewIntent");
        }

        super.onNewIntent(intent);
        setIntent(intent);

        assert mViewManager != null : "mViewManager == null";
        mViewManager.onActivityUpdated(this);
    }

    @MainThread
    @Override
    protected void onPause() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onPause");
        }

        super.onPause();
    }

    @MainThread
    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onActivityResult");
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startImageChooser(true);
                } else {
                    startImageChooser(false);
                }

                break;
            default:
                break;
        }
    }

    @MainThread
    @Override
    protected void onRestart() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onRestart");
        }

        super.onRestart();
    }

    @MainThread
    @Override
    protected void onResume() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onResume");
        }

        enableImmersiveFullScreenMode();
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    enableImmersiveFullScreenMode();
                }
            }
        });

        super.onResume();
    }

    @MainThread
    public void onRetryClick(@Nullable final View view) {
//        // TODO: Fix retry button. We need a way to call into the WebViewManager. :-(
//        final Button retryButton = findViewById(R.id.activity_zapic_retry);
//        if (retryButton == null || !retryButton.equals(view)) {
//            return;
//        }
//
//        if (mWebViewManager != null) {
//            showLoadingPage();
//            mWebViewManager.retryLoadWebPage();
//        }
    }

    @MainThread
    @Override
    protected void onStart() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onStart");
        }

        super.onStart();
        mStarted = true;
    }

    @MainThread
    @Override
    protected void onStop() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onStop");
        }

        super.onStop();
        mStarted = false;
    }

    @MainThread
    @Override
    public void onWindowFocusChanged(final boolean hasFocus) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onWindowFocusChanged");
        }

        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableImmersiveFullScreenMode();
        }
    }

    @MainThread
    void showImageChooser(@NonNull final ValueCallback<Uri[]> imageChooserCallback) {
        if (mImageChooserCallback != null) {
            mImageChooserCallback.onReceiveValue(null);
            mImageChooserCallback = null;
        }

        mImageChooserCallback = imageChooserCallback;

        final boolean hasCameraPermission = checkPermission(Manifest.permission.CAMERA, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (hasCameraPermission || !hasCameraPermissionInManifest()) {
                startImageChooser(true);
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    new AlertDialog.Builder(this)
                            .setTitle("Camera Permission")
                            .setMessage("Would you like to use the camera to take a new photo?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @MainThread
                                @Override
                                @RequiresApi(api = Build.VERSION_CODES.M)
                                public void onClick(DialogInterface dialog, int which) {
                                    requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startImageChooser(false);
                                }
                            })
                            .create()
                            .show();
                } else {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
                }
            }
        } else {
            startImageChooser(hasCameraPermission);
        }
    }

    @MainThread
    void showLoadingPage() {
        // Show progress bar.
        final ProgressBar progressBar = findViewById(R.id.activity_zapic_progress_bar);
        progressBar.setVisibility(View.VISIBLE);

        hideRetryPage();
        hideWebPage();
    }

    @MainThread
    void showRetryPage() {
        // Hide close button.
        final ImageButton closeButton = findViewById(R.id.activity_zapic_close);
        closeButton.setVisibility(View.VISIBLE);

        // Hide retry button.
        final RelativeLayout warningPanel = findViewById(R.id.activity_zapic_retry_container);
        warningPanel.setVisibility(View.VISIBLE);

        hideLoadingPage();
        hideWebPage();
    }

    @MainThread
    void showWebPage(@NonNull final WebView webView) {
        if (mWebView == webView) {
            return;
        }

        hideWebPage();
        mWebView = webView;

        final MutableContextWrapper webViewContext = (MutableContextWrapper) webView.getContext();
        webViewContext.setBaseContext(this);

        final FrameLayout layout = findViewById(R.id.activity_zapic_container);
        webView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        layout.addView(webView);

        if (mStarted) {
            webView.setY(layout.getHeight());
            webView.setVisibility(View.VISIBLE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                final WebSettings settings = mWebView.getSettings();
                settings.setOffscreenPreRaster(true);
            }

            mAnimation = webView.animate().y(0).setDuration(FADE_DURATION).withEndAction(new Runnable() {
                @Override
                public void run() {
                    layout.setBackgroundColor(Color.argb(255, 0, 0, 0));
                    hideLoadingPage();
                    hideRetryPage();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        final WebSettings settings = mWebView.getSettings();
                        settings.setOffscreenPreRaster(false);
                    }
                }
            });
        } else {
            webView.setY(0);
            webView.setVisibility(View.VISIBLE);

            layout.setBackgroundColor(Color.argb(255, 0, 0, 0));
            hideLoadingPage();
            hideRetryPage();
        }
    }

    @MainThread
    private void startImageChooser(final boolean includeCamera) {
        ArrayList<Intent> intents = new ArrayList<>();
        final PackageManager packageManager = getPackageManager();

        if (includeCamera) {
            // Find camera activities.
            final File imageDirectory = new File(this.getCacheDir(), "Zapic" + File.separator + "Share");
            if (imageDirectory.isDirectory() || imageDirectory.mkdirs()) {
                final File imageFile = new File(imageDirectory, "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".jpg");
                final String packageName = getPackageName();
                final Uri imageUri = FileProvider.getUriForFile(this.getApplicationContext(), packageName + ".zapic", imageFile);

                final Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                final List<ResolveInfo> activities = packageManager.queryIntentActivities(cameraIntent, 0);
                for (ResolveInfo activity : activities) {
                    final String activityPackageName = activity.activityInfo.packageName;
                    final String activityName = activity.activityInfo.name;

                    final Intent resolveIntent = new Intent(cameraIntent);
                    resolveIntent.setComponent(new ComponentName(activityPackageName, activityName));
                    resolveIntent.setPackage(activityPackageName);
                    resolveIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                        resolveIntent.setClipData(ClipData.newRawUri("", imageUri));
                        resolveIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    }

                    intents.add(resolveIntent);
                }

                if (intents.size() > 0) {
                    mImageUriForCamera = imageUri;
                } else {
                    mImageUriForCamera = null;
                }
            } else {
                mImageUriForCamera = null;
            }
        } else {
            mImageUriForCamera = null;
        }

        // Find library activities.
        final Intent libraryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        libraryIntent.addCategory(Intent.CATEGORY_OPENABLE);
        libraryIntent.setType("image/*");

        final List<ResolveInfo> activities = packageManager.queryIntentActivities(libraryIntent, 0);
        for (ResolveInfo activity : activities) {
            final String activityPackageName = activity.activityInfo.packageName;
            final String name = activity.activityInfo.name;

            final Intent resolveIntent = new Intent(libraryIntent);
            resolveIntent.setComponent(new ComponentName(activityPackageName, name));
            resolveIntent.setPackage(activityPackageName);
            intents.add(resolveIntent);
        }

        if (intents.size() > 0) {
            // Show an app chooser.
            Intent targetIntent = intents.get(intents.size() - 1);
            intents.remove(intents.size() - 1);
            Intent chooserIntent = Intent.createChooser(targetIntent, "Photo");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toArray(new Parcelable[intents.size()]));
            this.startActivityForResult(chooserIntent, IMAGE_REQUEST);
        } else {
            Toast.makeText(this, "A photo app could not be found", Toast.LENGTH_SHORT)
                    .show();
        }
    }
}
