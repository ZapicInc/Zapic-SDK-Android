package com.zapic.sdk.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

final class NotificationUtilities {
    static void showBanner(@NonNull final Context context, @NonNull final String title, @Nullable final String subtitle, @Nullable final Bitmap icon) {
        final LayoutInflater layoutInflater = LayoutInflater.from(context.getApplicationContext());
        @SuppressLint("InflateParams") final View layout = layoutInflater.inflate(R.layout.component_zapic_toast, null);

        TextView titleView = ViewCompat.requireViewById(layout, R.id.component_zapic_toast_title);
        titleView.setText(title);

        TextView subtitleView = ViewCompat.requireViewById(layout, R.id.component_zapic_toast_subtitle);
        if (subtitle == null) {
            final ViewGroup parent = (ViewGroup) subtitleView.getParent();
            parent.removeView(subtitleView);
        } else {
            titleView.setTypeface(titleView.getTypeface(), Typeface.BOLD);
            subtitleView.setText(subtitle);
        }

        ImageView imageView = ViewCompat.requireViewById(layout, R.id.component_zapic_toast_icon);
        if (icon == null) {
            imageView.setImageResource(R.drawable.zapic_logo_64dp);
        } else {
            imageView.setImageBitmap(icon);
        }

        final Toast toast = new Toast(context.getApplicationContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setGravity(Gravity.FILL_HORIZONTAL | Gravity.TOP, 0, 0);
        toast.setMargin(0, 0);
        toast.setView(layout);
        toast.show();
    }
}
