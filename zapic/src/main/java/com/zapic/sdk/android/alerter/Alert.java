package com.zapic.sdk.android.alerter;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zapic.sdk.android.R;

/**
 * Custom Alert View
 *
 * @author Kevin Murphy, Tapadoo, Dublin, Ireland, Europe, Earth.
 * @since 26/01/2016
 **/
public class Alert extends FrameLayout implements View.OnClickListener, Animation.AnimationListener, SwipeDismissTouchListener.DismissCallbacks {

    private static final int CLEAN_UP_DELAY_MILLIS = 100;

    /**
     * The amount of time the alert will be visible on screen in seconds
     */
    private static final long DISPLAY_TIME_IN_SECONDS = 3000;

    //UI
    private FrameLayout flClickShield;
    private FrameLayout flBackground;
    private TextView tvTitle;
    private TextView tvText;
    private ImageView ivIcon;
    private ViewGroup rlContainer;
    private boolean dismissable = true;

    private Animation slideInAnimation;
    private Animation slideOutAnimation;

    private OnShowAlertListener onShowListener;
    private OnHideAlertListener onHideListener;

    private long duration = DISPLAY_TIME_IN_SECONDS;

    private boolean enableInfiniteDuration;

    private Runnable runningAnimation;

    /**
     * Flag to ensure we only set the margins once
     */
    private boolean marginSet;
    /**
     * Flag to enable / disable haptic feedback
     */
    private boolean vibrationEnabled = true;

    /**
     * This is the default view constructor. It requires a Context, and holds a reference to it.
     * If not cleaned up properly, memory will leak.
     *
     * @param context The Activity Context
     */
    public Alert(@NonNull final Context context) {
        super(context, null, R.attr.alertStyle);
        initView();
    }

    /**
     * This is the default view constructor. It requires a Context, and holds a reference to it.
     * If not cleaned up properly, memory will leak.
     *
     * @param context The Activity Context
     * @param attrs   View Attributes
     */
    public Alert(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs, R.attr.alertStyle);
        initView();
    }

    /**
     * This is the default view constructor. It requires a Context, and holds a reference to it.
     * If not cleaned up properly, memory will leak.
     *
     * @param context      The Activity Context
     * @param attrs        View Attributes
     * @param defStyleAttr Styles
     */
    public Alert(@NonNull final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        inflate(getContext(), R.layout.alerter_alert_view, this);
        setHapticFeedbackEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.setTranslationZ(Integer.MAX_VALUE);
        }

        flBackground = findViewById(R.id.flAlertBackground);
        flClickShield = findViewById(R.id.flClickShield);
        ivIcon = findViewById(R.id.ivIcon);
        tvTitle = findViewById(R.id.tvTitle);
        tvText = findViewById(R.id.tvText);
        rlContainer = findViewById(R.id.rlContainer);

        flBackground.setOnClickListener(this);

        //Setup Enter & Exit Animations
        slideInAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_from_top);
        slideOutAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_out_to_top);
        slideInAnimation.setAnimationListener(this);

        //Set Animation to be Run when View is added to Window
        setAnimation(slideInAnimation);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (!marginSet) {
            marginSet = true;

            // Add a negative top margin to compensate for overshoot enter animation
            final ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) getLayoutParams();
            params.topMargin = getContext().getResources().getDimensionPixelSize(R.dimen.alerter_alert_negative_margin_top);
            requestLayout();
        }
    }

    // Release resources once view is detached.
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        slideInAnimation.setAnimationListener(null);
    }

    /* Override Methods */

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        performClick();
        return super.onTouchEvent(event);
    }

    @Override
    public void onClick(final View v) {
        if (!dismissable) {
            return;
        }
        hide();
    }

    @Override
    public void setOnClickListener(final OnClickListener listener) {
        flBackground.setOnClickListener(listener);
    }

    @Override
    public void setVisibility(final int visibility) {
        super.setVisibility(visibility);
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).setVisibility(visibility);
        }
    }

    /* Interface Method Implementations */

    @Override
    public void onAnimationStart(final Animation animation) {
        if (!isInEditMode()) {
            if (vibrationEnabled) {
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            }

            setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onAnimationEnd(final Animation animation) {
        if (onShowListener != null) {
            onShowListener.onShow();
        }

        startHideAnimation();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void startHideAnimation() {
        //Start the Handler to clean up the Alert
        if (!enableInfiniteDuration) {
            runningAnimation = new Runnable() {
                @Override
                public void run() {
                    hide();
                }
            };

            postDelayed(runningAnimation, duration);
        }
    }

    @Override
    public void onAnimationRepeat(final Animation animation) {
        //Ignore
    }

    /* Clean Up Methods */

    /**
     * Cleans up the currently showing alert view.
     */
    public void hide() {
        try {
            slideOutAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(final Animation animation) {
                    getAlertBackground().setOnClickListener(null);
                    getAlertBackground().setClickable(false);
                }

                @Override
                public void onAnimationEnd(final Animation animation) {
                    removeFromParent();
                }

                @Override
                public void onAnimationRepeat(final Animation animation) {
                    //Ignore
                }
            });

            startAnimation(slideOutAnimation);
        } catch (Exception ex) {
            Log.e(getClass().getSimpleName(), Log.getStackTraceString(ex));
        }
    }

    /**
     * Removes Alert View from its Parent Layout
     */
    void removeFromParent() {
        clearAnimation();
        setVisibility(View.GONE);

        postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (getParent() == null) {
                        Log.e(getClass().getSimpleName(), "getParent() returning Null");
                    } else {
                        try {
                            ((ViewGroup) getParent()).removeView(Alert.this);

                            if (getOnHideListener() != null) {
                                getOnHideListener().onHide();
                            }
                        } catch (Exception ex) {
                            Log.e(getClass().getSimpleName(), "Cannot remove from parent layout");
                        }
                    }
                } catch (Exception ex) {
                    Log.e(getClass().getSimpleName(), Log.getStackTraceString(ex));
                }
            }
        }, CLEAN_UP_DELAY_MILLIS);
    }

    /* Setters and Getters */

    /**
     * Sets the Alert Background colour
     *
     * @param color The qualified colour integer
     */
    public void setAlertBackgroundColor(@ColorInt final int color) {
        flBackground.setBackgroundColor(color);
    }

    /**
     * Sets the Alert Background Drawable Resource
     *
     * @param resource The qualified drawable integer
     */
    public void setAlertBackgroundResource(@DrawableRes final int resource) {
        flBackground.setBackgroundResource(resource);
    }

    /**
     * Sets the Alert Background Drawable
     *
     * @param drawable The qualified drawable
     */
    public void setAlertBackgroundDrawable(final Drawable drawable) {
        flBackground.setBackground(drawable);
    }

    public int getContentGravity() {
        return ((LayoutParams) rlContainer.getLayoutParams()).gravity;
    }

    /**
     * Sets the Gravity of the Alert
     *
     * @param contentGravity Gravity of the Alert
     */
    public void setContentGravity(final int contentGravity) {
        final LinearLayout.LayoutParams paramsTitle
                = (LinearLayout.LayoutParams) tvTitle.getLayoutParams();
        paramsTitle.gravity = contentGravity;
        tvTitle.setLayoutParams(paramsTitle);

        final LinearLayout.LayoutParams paramsText
                = (LinearLayout.LayoutParams) tvText.getLayoutParams();
        paramsText.gravity = contentGravity;
        tvText.setLayoutParams(paramsText);
    }

    /**
     * Disable touches while the Alert is showing
     */
    public void disableOutsideTouch() {
        flClickShield.setClickable(true);
    }

    public FrameLayout getAlertBackground() {
        return flBackground;
    }

    public TextView getTitle() {
        return tvTitle;
    }

    /**
     * Sets the Title of the Alert
     *
     * @param titleId String resource id of the Alert title
     */
    public void setTitle(@StringRes final int titleId) {
        setTitle(getContext().getString(titleId));
    }

    /**
     * Sets the Title of the Alert
     *
     * @param title String object to be used as the Alert title
     */
    public void setTitle(@NonNull final String title) {
        if (!TextUtils.isEmpty(title)) {
            tvTitle.setVisibility(VISIBLE);
            tvTitle.setText(title);
        }
    }

    /**
     * Set the Title's text appearance of the Title
     *
     * @param textAppearance The style resource id
     */
    @SuppressWarnings("deprecation")
    public void setTitleAppearance(@StyleRes final int textAppearance) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tvTitle.setTextAppearance(textAppearance);
        } else {
            tvTitle.setTextAppearance(tvTitle.getContext(), textAppearance);
        }
    }

    /**
     * Set the Title's typeface
     *
     * @param typeface The typeface to use
     */
    public void setTitleTypeface(@NonNull final Typeface typeface) {
        tvTitle.setTypeface(typeface);
    }

    /**
     * Set the Text's typeface
     *
     * @param typeface The typeface to use
     */
    public void setTextTypeface(@NonNull final Typeface typeface) {
        tvText.setTypeface(typeface);
    }

    public TextView getText() {
        return tvText;
    }

    /**
     * Sets the Text of the Alert
     *
     * @param textId String resource id of the Alert text
     */
    public void setText(@StringRes final int textId) {
        setText(getContext().getString(textId));
    }

    /**
     * Sets the Text of the Alert
     *
     * @param text String resource id of the Alert text
     */
    public void setText(final String text) {
        if (!TextUtils.isEmpty(text)) {
            tvText.setVisibility(VISIBLE);
            tvText.setText(text);
        }
    }

    /**
     * Set the Text's text appearance of the Title
     *
     * @param textAppearance The style resource id
     */
    @SuppressWarnings("deprecation")
    public void setTextAppearance(@StyleRes final int textAppearance) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tvText.setTextAppearance(textAppearance);
        } else {
            tvText.setTextAppearance(tvText.getContext(), textAppearance);
        }
    }

    public ImageView getIcon() {
        return ivIcon;
    }

    /**
     * Set the icon color for the Alert
     *
     * @param color Color int
     */
    public void setIconColorFilter(@ColorInt final int color) {
        if (ivIcon != null) {
            ivIcon.setColorFilter(color);
        }
    }

    /**
     * Set the icon color for the Alert
     *
     * @param colorFilter ColorFilter
     */
    public void setIconColorFilter(@NonNull final ColorFilter colorFilter) {
        if (ivIcon != null) {
            ivIcon.setColorFilter(colorFilter);
        }
    }

    /**
     * Set the icon color for the Alert
     *
     * @param color Color int
     * @param mode  PorterDuff.Mode
     */
    public void setIconColorFilter(@ColorInt final int color, final PorterDuff.Mode mode) {
        if (ivIcon != null) {
            ivIcon.setColorFilter(color, mode);
        }
    }

    /**
     * Set the inline icon for the Alert
     *
     * @param bitmap Bitmap image of the icon to use in the Alert.
     */
    public void setIcon(@NonNull final Bitmap bitmap) {
        ivIcon.setImageBitmap(bitmap);
    }

    /**
     * Set the inline icon for the Alert
     *
     * @param drawable Drawable image of the icon to use in the Alert.
     */
    public void setIcon(@NonNull final Drawable drawable) {
        ivIcon.setImageDrawable(drawable);
    }

    /**
     * Set whether to show the icon in the alert or not
     *
     * @param showIcon True to show the icon, false otherwise
     */
    public void showIcon(final boolean showIcon) {
        ivIcon.setVisibility(showIcon ? View.VISIBLE : View.GONE);
    }

    /**
     * Set whether to enable swipe to dismiss or not
     */
    public void enableSwipeToDismiss() {
        flBackground.setOnTouchListener(new SwipeDismissTouchListener(flBackground, null, new SwipeDismissTouchListener.DismissCallbacks() {
            @Override
            public boolean canDismiss(final Object token) {
                return true;
            }

            @Override
            public void onDismiss(final View view, final Object token) {
                removeFromParent();
            }

            @Override
            public void onTouch(final View view, final boolean touch) {
                // Ignore
            }
        }));
    }

    /**
     * Set if the alerter is dismissable or not
     *
     * @param dismissable True if alert can be dismissed
     */
    public void setDismissable(final boolean dismissable) {
        this.dismissable = dismissable;
    }

    /**
     * Get if the alert is dismissable
     *
     * @return True if the alert is dismissable, false otherwise
     */
    public boolean isDismissable() {
        return dismissable;
    }

    /**
     * Get the Alert's on screen duration
     *
     * @return The given duration, defaulting to 3000 milliseconds
     */
    public long getDuration() {
        return duration;
    }

    /**
     * Set the alert's on screen duation
     *
     * @param duration The duration of alert on screen
     */
    public void setDuration(final long duration) {
        this.duration = duration;
    }

    /**
     * Set if the duration of the alert is infinite
     *
     * @param enableInfiniteDuration True if the duration of the alert is infinite
     */
    public void setEnableInfiniteDuration(final boolean enableInfiniteDuration) {
        this.enableInfiniteDuration = enableInfiniteDuration;
    }

    /**
     * Set the alert's listener to be fired on the alert being fully shown
     *
     * @param listener Listener to be fired
     */
    public void setOnShowListener(@NonNull final OnShowAlertListener listener) {
        this.onShowListener = listener;
    }

    /**
     * Set the alert's listener to be fired on the alert being fully hidden
     *
     * @param listener Listener to be fired
     */
    public void setOnHideListener(@NonNull final OnHideAlertListener listener) {
        this.onHideListener = listener;
    }

    /**
     * Enable or Disable haptic feedback
     *
     * @param vibrationEnabled True to enable, false to disable
     */
    public void setVibrationEnabled(final boolean vibrationEnabled) {
        this.vibrationEnabled = vibrationEnabled;
    }

    @Override
    public boolean canDismiss(final Object token) {
        return true;
    }

    @Override
    public void onDismiss(final View view, final Object token) {
        flClickShield.removeView(flBackground);
    }

    @Override
    public void onTouch(final View view, final boolean touch) {
        if (touch) {
            removeCallbacks(runningAnimation);
        } else {
            startHideAnimation();
        }
    }

    /**
     * Get the on hide listener
     *
     * @return On hide listener
     */
    public OnHideAlertListener getOnHideListener() {
        return onHideListener;
    }
}
