package com.zapic.androiddemo;

import android.app.Activity;
import android.graphics.PointF;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.zapic.android.sdk.Zapic;

public class MainActivity extends Activity {
    /**
     * The minimum X or Y delta needed to update {@link #totalTouchDistance}.
     */
    private static final float TOUCH_TOLERANCE = 4f;

    /**
     * The last touch point.
     */
    private PointF lastTouchPoint;

    /**
     *
     */
    private double totalTouchDistance;

    public MainActivity() {
        this.lastTouchPoint = null;
        this.totalTouchDistance = 0f;
    }

    /**
     * Enables an immersive full-screen mode. This hides the system status and navigation bars until
     * the user swipes in from the edges of the screen.
     */
    @MainThread
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

    @MainThread
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the system status and navigation bars.
        this.enableImmersiveFullScreenMode();

        // Render the page.
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
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return MainActivity.this.onTouch(motionEvent);
            }
        });

        // Start Zapic.
        Zapic.attachFragment(this);
    }

    @MainThread
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

    @MainThread
    @Override
    public void onWindowFocusChanged(final boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // Hide the system status and navigation bars.
        if (hasFocus) {
            this.enableImmersiveFullScreenMode();
        }
    }
}
