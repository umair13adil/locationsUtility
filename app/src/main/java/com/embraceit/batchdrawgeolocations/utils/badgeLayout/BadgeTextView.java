package com.embraceit.batchdrawgeolocations.utils.badgeLayout;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.view.Display;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.embraceit.batchdrawgeolocations.R;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static android.util.TypedValue.applyDimension;

/**
 * Created by Aaqib on 12/5/2016.
 */

public class BadgeTextView {

    public static class Builder{

        public int textBackgroundColor; // TRANSPARENT = 0;
        public int textColor; // TRANSPARENT = 0;
        public Builder(){

        }
        public Builder textBackgroundColor(int textBackgroundColor){
            this.textBackgroundColor = textBackgroundColor;
            return this;
        }

        public Builder textColor(int textColor){
            this.textColor = textColor;
            return this;
        }

    }


    public static void update(final Activity activity, final MenuItem menu, Builder builder) {
        update(activity, menu, builder, null);

    }

    public static void update(final Activity activity, final FrameLayout frameLayout, Builder builder) {
        update(frameLayout, builder);

    }

    public static void update(final Activity activity, final MenuItem menu, Builder builder, final ActionItemBadgeListener listener) {
        if (menu == null) return;
        FrameLayout badge;
        BadgeView badgeDrawableView;
        BadgeView badgeTextView;
        ImageView imageView;

        badge = (FrameLayout) menu.getActionView();

        badgeDrawableView = badge.findViewById(R.id.menu_badge);
        badgeTextView = badge.findViewById(R.id.menu_badge_text);
        imageView = badge.findViewById(R.id.menu_badge_icon);
        badgeDrawableView.setVisibility(View.GONE);
        imageView.setVisibility(View.GONE);
        badgeTextView.setVisibility(View.VISIBLE);

        if (builder != null && builder.textBackgroundColor != Color.TRANSPARENT) {
            //badgeTextView.setBackgroundColor(builder.textBackgroundColor);
            imageView.setColorFilter(builder.textBackgroundColor);
        }

        if (builder != null && builder.textColor != Color.TRANSPARENT) {
            badgeTextView.setTextColor(builder.textColor);
        }


        //Bind onOptionsItemSelected to the activity
        if (activity != null) {
            badge.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean consumed = false;
                    if (listener != null) {
                        consumed = listener.onOptionsItemSelected(menu);
                    }
                    if (!consumed) {
                        activity.onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, menu);
                    }
                }
            });

            badge.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Display display = activity.getWindowManager().getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    int width = size.x;
                    Toast toast = Toast.makeText(activity, menu.getTitle(), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.TOP, width / 5, convertDpToPx(activity, 48));
                    toast.show();
                    return true;
                }
            });
        }

        menu.setVisible(true);
    }

    public static void update(final FrameLayout frameLayout, Builder builder) {
        FrameLayout badge;
        BadgeView badgeDrawableView;
        BadgeView badgeTextView;
        ImageView imageView;

        badge = frameLayout;

        badgeDrawableView = badge.findViewById(R.id.menu_badge);
        badgeTextView = badge.findViewById(R.id.menu_badge_text);
        imageView = badge.findViewById(R.id.menu_badge_icon);
        badgeDrawableView.setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);
        badgeTextView.setVisibility(View.VISIBLE);

        if (builder != null && builder.textBackgroundColor != Color.TRANSPARENT) {
            //badgeTextView.setBackgroundColor(builder.textBackgroundColor);
            imageView.setColorFilter(builder.textBackgroundColor);
        }

        if (builder != null && builder.textColor != Color.TRANSPARENT) {
            badgeTextView.setTextColor(builder.textColor);
        }
    }

    public static BadgeView getBadgeTextView(MenuItem menu) {
        if (menu == null) {
            return null;
        }
        FrameLayout badge = (FrameLayout) menu.getActionView();
        BadgeView badgeView = badge.findViewById(R.id.menu_badge_text);
        return badgeView;
    }

    public static BadgeView getBadgeTextView(FrameLayout frameLayout) {
        FrameLayout badge = frameLayout;
        BadgeView badgeView = badge.findViewById(R.id.menu_badge_text);
        return badgeView;
    }

    public static void hide(MenuItem menu) {
        menu.setVisible(false);
    }


    public interface ActionItemBadgeListener {
        boolean onOptionsItemSelected(MenuItem menu);
    }

    public static int convertDpToPx(Context context, float dp) {
        return (int) applyDimension(COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static void setBackgroundCompat(View v, Drawable d) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            v.setBackgroundDrawable(d);
        } else {
            v.setBackground(d);
        }
    }

    public static String formatNumber(int value, boolean limitLength) {
        if (value < 0) {
            return "-" + formatNumber(-value, limitLength);
        } else if (value < 100) {
            return Long.toString(value);
        } else {
            return "99+";
        }

    }
}
