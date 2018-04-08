package com.zapic.androiddemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.squareup.seismic.ShakeDetector;
import com.zapic.sdk.android.AppSourceConfig;
import com.zapic.sdk.android.Zapic;

public class MainActivity extends Activity implements
        ShakeDetector.Listener,
        SharedPreferences.OnSharedPreferenceChangeListener {
    /**
     * The minimum X or Y delta needed to update {@link #totalTouchDistance}.
     */
    private static final float TOUCH_TOLERANCE = 4f;

    private PointF lastTouchPoint;

    private SensorManager sensorManager;

    private ShakeDetector shakeDetector;

    private SharedPreferences sharedPreferences;

    private double totalTouchDistance;

    public MainActivity() {
        this.lastTouchPoint = null;
        this.sensorManager = null;
        this.shakeDetector = null;
        this.totalTouchDistance = 0f;
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

    @Override
    public void hearShake() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(final Bundle savedInstanceState) {
        // Configure Zapic. Note: This is only for debugging! Do not use this in production apps!
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        boolean cachePref = this.sharedPreferences.getBoolean(SettingsActivity.KEY_CACHE, true);
        if (cachePref) {
            AppSourceConfig.enableCache();
        } else {
            AppSourceConfig.disableCache();
        }

        String urlPref = this.sharedPreferences.getString(SettingsActivity.KEY_URL, "https://app.zapic.net");
        AppSourceConfig.setUrl(urlPref);

        // Hide the system status and navigation bars.
        this.enableImmersiveFullScreenMode();
        this.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(final int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    MainActivity.this.enableImmersiveFullScreenMode();
                }
            }
        });

        // Render the page.
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);

        final RelativeLayout zapicButton = this.findViewById(R.id.activity_main_zapic_button);
        zapicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Zapic.show(MainActivity.this);
            }
        });

        final RelativeLayout challengesButton = this.findViewById(R.id.activity_main_challenges_button);
        challengesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Zapic.show(MainActivity.this, "challenges");
            }
        });

        this.findViewById(R.id.activity_main_container).setOnTouchListener(new View.OnTouchListener() {
            @Override
            @SuppressLint("ClickableViewAccessibility")
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return MainActivity.this.onTouch(motionEvent);
            }
        });

        // Start Zapic.
        Zapic.attachFragment(this);

        // Get accelerometer.
        this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (this.sensorManager != null) {
            this.shakeDetector = new ShakeDetector(this);
        }
    }

    @Override
    protected void onPause() {
        // Disable shake detector.
        if (this.shakeDetector != null) {
            this.shakeDetector.stop();
        }

        // Disable preference change listener.
        if (this.sharedPreferences != null) {
            this.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }

        super.onPause();
    }

    @Override
    protected void onResume() {
        // Hide the system status and navigation bars.
        this.enableImmersiveFullScreenMode();
        this.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(final int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    MainActivity.this.enableImmersiveFullScreenMode();
                }
            }
        });

        // Enable shake detector.
        if (this.shakeDetector != null) {
            this.shakeDetector.start(this.sensorManager);
        }

        // Enable preference change listener.
        if (this.sharedPreferences != null) {
            this.sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }

        super.onResume();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SettingsActivity.KEY_CACHE)) {
            boolean cachePref = sharedPreferences.getBoolean(SettingsActivity.KEY_CACHE, true);
            if (cachePref) {
                AppSourceConfig.enableCache();
            } else {
                AppSourceConfig.disableCache();
            }
        }

        if (key.equals(SettingsActivity.KEY_URL)) {
            String urlPref = sharedPreferences.getString(SettingsActivity.KEY_URL, "https://app.zapic.net");
            AppSourceConfig.setUrl(urlPref);
        }
    }

    private boolean onTouch(@NonNull final MotionEvent motionEvent) {
        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                this.lastTouchPoint = new PointF(motionEvent.getX(), motionEvent.getY());
                this.totalTouchDistance = 0f;
                break;
            case MotionEvent.ACTION_MOVE:
                if (this.lastTouchPoint == null) {
                    this.lastTouchPoint = new PointF(motionEvent.getX(), motionEvent.getY());
                    this.totalTouchDistance = 0f;
                } else {
                    float dx = Math.abs(this.lastTouchPoint.x - motionEvent.getX());
                    float dy = Math.abs(this.lastTouchPoint.y - motionEvent.getY());
                    if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                        this.lastTouchPoint.set(motionEvent.getX(), motionEvent.getY());
                        this.totalTouchDistance += Math.sqrt((dx * dx) + (dy * dy));
                    }
                }

                break;
            case MotionEvent.ACTION_UP:
                if (this.lastTouchPoint != null) {
                    float dx = Math.abs(this.lastTouchPoint.x - motionEvent.getX());
                    float dy = Math.abs(this.lastTouchPoint.y - motionEvent.getY());
                    long distance = Math.round(this.totalTouchDistance + Math.sqrt((dx * dx) + (dy * dy)));

                    // Submit gameplay event to Zapic.
                    String parameters = "{\"DISTANCE\":\"" + String.valueOf(distance) + "\"}";
                    Zapic.submitEvent(this, parameters);

                    this.lastTouchPoint = null;
                    this.totalTouchDistance = 0f;
                }

                break;
            default:
                return false;
        }

        return true;
    }

    @Override
    public void onWindowFocusChanged(final boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // Hide the system status and navigation bars.
        if (hasFocus) {
            this.enableImmersiveFullScreenMode();
        }
    }
}
