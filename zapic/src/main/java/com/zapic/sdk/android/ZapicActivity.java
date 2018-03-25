package com.zapic.sdk.android;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.CheckResult;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.transition.Slide;
import android.util.Log;
import android.view.View;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ZapicActivity extends FragmentActivity {
    /**
     * Identifies an action request to capture an image from the device camera.
     */
    private static final int IMAGE_CAMERA_REQUEST = 1000;

    /**
     * Identifies a permission request to capture an image from the device camera.
     */
    private static final int IMAGE_CAMERA_PERMISSION_REQUEST = 1001;

    /**
     * Identifies an action request to import an image from the media library.
     */
    private static final int IMAGE_LIBRARY_REQUEST = 1002;

    /**
     * Identifies a permission request to import an image from the media library.
     */
    private static final int IMAGE_LIBRARY_PERMISSION_REQUEST = 1003;

    /**
     * The {@link Intent} parameter that identifies the Zapic JavaScript application page to open.
     */
    @NonNull
    private static final String PAGE_PARAMETER = "page";

    /**
     * The tag used to identify log messages.
     */
    @NonNull
    private static final String TAG = "ZapicActivity";

    /**
     * The file to use to capture an image from the device camera.
     */
    @Nullable
    private File mImageCameraFile;

    /**
     * A strong reference to the {@link WebViewManager} instance.
     */
    @Nullable
    private WebViewManager mWebViewManager;

    /**
     * Creates a new {@link ZapicActivity} instance.
     */
    public ZapicActivity() {
        this.mImageCameraFile = null;
        this.mWebViewManager = null;
    }

    /**
     * Creates an {@link Intent} that starts a {@link ZapicActivity} and opens the default Zapic
     * JavaScript application page.
     *
     * @param gameActivity The game's activity.
     * @return The {@link Intent}.
     */
    @MainThread
    @CheckResult
    @NonNull
    public static Intent createIntent(@NonNull final Activity gameActivity) {
        return ZapicActivity.createIntent(gameActivity, "default");
    }

    /**
     * Creates an {@link Intent} that starts a {@link ZapicActivity} and opens the specified Zapic
     * JavaScript application page.
     *
     * @param gameActivity The game's activity.
     * @param page         The Zapic JavaScript application page to open.
     * @return The {@link Intent}.
     */
    @MainThread
    @CheckResult
    @NonNull
    public static Intent createIntent(@NonNull final Activity gameActivity, @NonNull final String page) {
        final Intent intent = new Intent(gameActivity, ZapicActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(PAGE_PARAMETER, page);
        return intent;
    }

    /**
     * Enables an immersive full-screen mode. This hides the system status and navigation bars until
     * the user swipes in from the edges of the screen.
     */
    private void enableImmersiveFullScreenMode() {
        this.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LOW_PROFILE);
    }

    @NonNull
    String getPageParameter() {
        final Bundle parameters = this.getIntent().getExtras();
        if (parameters == null) {
            return "default";
        }

        final String page = parameters.getString(PAGE_PARAMETER);
        if (page == null || page.equals("")) {
            return "default";
        }

        return page;
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @NonNull final Intent data) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onActivityResult");
        }

        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case IMAGE_CAMERA_REQUEST: {
                final Uri[] files = resultCode == RESULT_OK && this.mImageCameraFile != null
                        ? new Uri[]{FileProvider.getUriForFile(this.getApplicationContext(), this.getApplicationContext().getPackageName() + ".zapic", this.mImageCameraFile)}
                        : null;
                if (files == null) {
                    assert this.mWebViewManager != null : "mWebViewManager is null";
                    this.mWebViewManager.cancelImageUpload();
                } else {
                    assert this.mWebViewManager != null : "mWebViewManager is null";
                    this.mWebViewManager.submitImageUpload(files);
                }

                this.mImageCameraFile = null;
                break;
            }
            case IMAGE_LIBRARY_REQUEST: {
                final Uri[] files = resultCode == RESULT_OK && data.getData() != null
                        ? new Uri[]{data.getData()}
                        : null;
                if (files == null) {
                    assert this.mWebViewManager != null : "mWebViewManager is null";
                    this.mWebViewManager.cancelImageUpload();
                } else {
                    assert this.mWebViewManager != null : "mWebViewManager is null";
                    this.mWebViewManager.submitImageUpload(files);
                }

                break;
            }
            default:
                break;
        }
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreate");
        }

        this.enableImmersiveFullScreenMode();
        this.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(final int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    ZapicActivity.this.enableImmersiveFullScreenMode();
                }
            }
        });

        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_zapic);
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final Slide slide = new Slide();
            slide.setDuration(150);
            this.getWindow().setEnterTransition(slide);
        }

        Zapic.attachFragment(this);

        assert this.mWebViewManager == null : "mWebViewManager is not null";
        this.mWebViewManager = WebViewManager.getInstance();
        this.mWebViewManager.onActivityCreated(this);
    }

    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onDestroy");
        }

        super.onDestroy();

        Zapic.detachFragment(this);

        assert this.mWebViewManager != null : "mWebViewManager is null";
        this.mWebViewManager.onActivityDestroyed(this);
        this.mWebViewManager = null;
    }

    @Override
    protected void onNewIntent(@NonNull final Intent intent) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onNewIntent");
        }

        super.onNewIntent(intent);
        this.setIntent(intent);
    }

    @Override
    protected void onPause() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onPause");
        }

        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onRequestPermissionsResult");
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case IMAGE_CAMERA_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.startImageCameraActivity();
                } else {
                    assert this.mWebViewManager != null : "mWebViewManager is null";
                    this.mWebViewManager.cancelImageUpload();
                }

                break;
            case IMAGE_LIBRARY_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.startImageLibraryActivity();
                } else {
                    assert this.mWebViewManager != null : "mWebViewManager is null";
                    this.mWebViewManager.cancelImageUpload();
                }

                break;
            default:
                break;
        }
    }

    @Override
    protected void onRestart() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onRestart");
        }

        super.onRestart();
    }

    @Override
    protected void onResume() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onResume");
        }

        this.enableImmersiveFullScreenMode();
        this.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(final int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    ZapicActivity.this.enableImmersiveFullScreenMode();
                }
            }
        });

        super.onResume();
    }

    @Override
    protected void onStart() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onStart");
        }

        super.onStart();

        assert this.mWebViewManager != null : "mWebViewManager is null";
        this.mWebViewManager.onActivityStarted();
    }

    @Override
    protected void onStop() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onStop");
        }

        super.onStop();

        assert this.mWebViewManager != null : "mWebViewManager is null";
        this.mWebViewManager.onActivityStopped();
    }

    @Override
    public void onWindowFocusChanged(final boolean hasFocus) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onWindowFocusChanged");
        }

        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            this.enableImmersiveFullScreenMode();
        }
    }

    private void showImagePermissionPrompt(final int permission) {
        int message;
        switch (permission) {
            case IMAGE_CAMERA_PERMISSION_REQUEST:
                message = R.string.zapic_activity_image_permission_camera_message;
                break;
            case IMAGE_LIBRARY_PERMISSION_REQUEST:
                message = R.string.zapic_activity_image_permission_library_message;
                break;
            default:
                return;
        }

        final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(@NonNull final DialogInterface dialog, final int which) {
                if (which == AlertDialog.BUTTON_POSITIVE) {
                    ZapicActivity.this.startSettingsActivity();
                }

                dialog.dismiss();
            }
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.zapic_activity_image_permission_title)
                .setMessage(message)
                .setPositiveButton(R.string.zapic_activity_image_permission_submit, dialogClickListener)
                .create()
                .show();
    }

    void showImagePrompt() {
        final AtomicBoolean handled = new AtomicBoolean(false);

        final DialogInterface.OnDismissListener dialogDismissListener = new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(@NonNull final DialogInterface dialog) {
                if (handled.getAndSet(true)) {
                    return;
                }

                assert ZapicActivity.this.mWebViewManager != null : "mWebViewManager is null";
                ZapicActivity.this.mWebViewManager.cancelImageUpload();
            }
        };

        final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(@NonNull final DialogInterface dialog, final int which) {
                if (handled.getAndSet(true)) {
                    return;
                }

                switch (which) {
                    case 0: {
                        // Check if the camera permission is declared in the manifest.
                        boolean hasDeclaredCameraPermission = false;
                        try {
                            final PackageInfo packageInfo = ZapicActivity.this.getApplicationContext().getPackageManager().getPackageInfo(ZapicActivity.this.getApplicationContext().getPackageName(), PackageManager.GET_PERMISSIONS);
                            final String[] permissions = packageInfo.requestedPermissions;
                            if (permissions != null) {
                                for (String permission : permissions) {
                                    if (permission.equals(Manifest.permission.CAMERA)) {
                                        hasDeclaredCameraPermission = true;
                                        break;
                                    }
                                }
                            }
                        } catch (NameNotFoundException ignored) {
                        }

                        // The camera permission is automatically granted if the camera permission is *not*
                        // declared in the manifest.
                        if (!hasDeclaredCameraPermission || ContextCompat.checkSelfPermission(ZapicActivity.this.getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            ZapicActivity.this.startImageCameraActivity();
                        } else if (ActivityCompat.shouldShowRequestPermissionRationale(ZapicActivity.this, Manifest.permission.CAMERA)) {
                            assert ZapicActivity.this.mWebViewManager != null : "mWebViewManager is null";
                            ZapicActivity.this.mWebViewManager.cancelImageUpload();
                            ZapicActivity.this.showImagePermissionPrompt(IMAGE_CAMERA_PERMISSION_REQUEST);
                        } else {
                            ActivityCompat.requestPermissions(ZapicActivity.this, new String[]{Manifest.permission.CAMERA}, IMAGE_CAMERA_PERMISSION_REQUEST);
                        }

                        break;
                    }
                    case 1:
                        if (ContextCompat.checkSelfPermission(ZapicActivity.this.getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                            ZapicActivity.this.startImageLibraryActivity();
                        } else if (ActivityCompat.shouldShowRequestPermissionRationale(ZapicActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                            assert ZapicActivity.this.mWebViewManager != null : "mWebViewManager is null";
                            ZapicActivity.this.mWebViewManager.cancelImageUpload();
                            ZapicActivity.this.showImagePermissionPrompt(IMAGE_LIBRARY_PERMISSION_REQUEST);
                        } else {
                            ActivityCompat.requestPermissions(ZapicActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, IMAGE_LIBRARY_PERMISSION_REQUEST);
                        }

                        break;
                    default:
                        assert ZapicActivity.this.mWebViewManager != null : "mWebViewManager is null";
                        ZapicActivity.this.mWebViewManager.cancelImageUpload();
                        break;
                }

                dialog.dismiss();
            }
        };

        new AlertDialog
                .Builder(this)
                .setTitle(R.string.zapic_activity_image_source_title)
                .setItems(R.array.zapic_activity_image_source_items, dialogClickListener)
                .setNegativeButton(R.string.zapic_activity_image_source_cancel, dialogClickListener)
                .setOnDismissListener(dialogDismissListener)
                .create()
                .show();
    }

    private void startImageCameraActivity() {
        final File filesDir = this.getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (filesDir == null) {
            assert this.mWebViewManager != null : "mWebViewManager is null";
            this.mWebViewManager.cancelImageUpload();

            Toast.makeText(this.getApplicationContext(), R.string.zapic_activity_folder_error, Toast.LENGTH_SHORT).show();
        } else {
            this.mImageCameraFile = new File(filesDir.getAbsolutePath() + File.separator + "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".jpg");
            final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(this.getApplicationContext(), this.getApplicationContext().getPackageName() + ".zapic", this.mImageCameraFile));
            try {
                this.startActivityForResult(intent, IMAGE_CAMERA_REQUEST);
            } catch (ActivityNotFoundException e) {
                assert this.mWebViewManager != null : "mWebViewManager is null";
                this.mWebViewManager.cancelImageUpload();

                Toast.makeText(this.getApplicationContext(), R.string.zapic_activity_camera_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startImageLibraryActivity() {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        try {
            this.startActivityForResult(intent, IMAGE_LIBRARY_REQUEST);
        } catch (ActivityNotFoundException e) {
            assert this.mWebViewManager != null : "mWebViewManager is null";
            this.mWebViewManager.cancelImageUpload();

            Toast.makeText(this.getApplicationContext(), R.string.zapic_activity_library_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void startSettingsActivity() {
        final Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", this.getApplicationContext().getPackageName(), null));
        try {
            this.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this.getApplicationContext(), R.string.zapic_activity_settings_error, Toast.LENGTH_SHORT).show();
        }
    }
}
