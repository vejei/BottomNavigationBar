package io.github.vejei.bottomnavigationbar;

import android.annotation.TargetApi;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.util.StateSet;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

final class RippleUtils {
    private static final int[] SELECTED_PRESSED_STATE_SET = {
            android.R.attr.state_selected, android.R.attr.state_pressed,
    };
    private static final int[] SELECTED_STATE_SET = {
            android.R.attr.state_selected,
    };
    private static final int[] PRESSED_STATE_SET = {
            android.R.attr.state_pressed,
    };

    static ColorStateList convertToRippleDrawableColor(@Nullable ColorStateList rippleColor) {
        int size = 2;

        final int[][] states = new int[size][];
        final int[] colors = new int[size];
        int i = 0;

        // Ideally we would define a different composite color for each state, but that causes the
        // ripple animation to abort prematurely.
        // So we only allow two base states: selected, and non-selected. For each base state, we only
        // base the ripple composite on its pressed state.

        // Selected base state.
        states[i] = SELECTED_STATE_SET;
        colors[i] = getColorForState(rippleColor, SELECTED_PRESSED_STATE_SET);
        i++;

        // Non-selected base state.
        states[i] = StateSet.NOTHING;
        colors[i] = getColorForState(rippleColor, PRESSED_STATE_SET);

        return new ColorStateList(states, colors);
    }

    @ColorInt
    private static int getColorForState(@Nullable ColorStateList rippleColor, int[] state) {
        int color;
        if (rippleColor != null) {
            color = rippleColor.getColorForState(state, rippleColor.getDefaultColor());
        } else {
            color = Color.TRANSPARENT;
        }
        return doubleAlpha(color);
    }

    @ColorInt
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static int doubleAlpha(@ColorInt int color) {
        int alpha = Math.min(2 * Color.alpha(color), 255);
        return ColorUtils.setAlphaComponent(color, alpha);
    }
}
