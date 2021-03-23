package io.github.vejei.bottomnavigationbar;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BadgeDrawable extends Drawable {
    private static final int MAX_NUMBER = 999;
    private static final String EXCEED_MAX_BADGE_NUMBER_SUFFIX = "+";
    private static final int MAX_CIRCULAR_BADGE_NUMBER = 9;

    private final Context context;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Rect anchorBounds = new Rect();
    private final RectF backgroundBounds = new RectF();
    private final Rect textBounds = new Rect();

    private int number;
    private String numberText;

    private final int withoutTextCornerRadius;
    private final int withTextCornerRadius;
    private float backgroundCenterX;
    private float backgroundCenterY;
    private float backgroundHalfWidth;
    private float backgroundHalfHeight;
    private final int textHorizontalPadding;

    public BadgeDrawable(Context context) {
        this.context = context;

        Resources resources = context.getResources();

        withoutTextCornerRadius = resources.getDimensionPixelOffset(
                R.dimen.bnb_badge_without_text_corner_radius);
        withTextCornerRadius = resources.getDimensionPixelOffset(
                R.dimen.bnb_badge_with_text_corner_radius);
        textHorizontalPadding = resources.getDimensionPixelOffset(
                R.dimen.bnb_badge_long_text_horizontal_padding);

        textPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.bnb_badge_text_size));
        textPaint.setColor(Color.WHITE);
        paint.setColor(Color.RED);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty() || getAlpha() == 0 || !isVisible()) {
            return;
        }

        canvas.drawRoundRect(backgroundBounds, backgroundHalfHeight, backgroundHalfHeight, paint);
        if (hasNumber()) {
            textPaint.getTextBounds(numberText, 0, numberText.length(), textBounds);
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(numberText, backgroundCenterX,
                    backgroundCenterY - textBounds.exactCenterY(), textPaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        textPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return (int) backgroundBounds.width();
    }

    @Override
    public int getIntrinsicHeight() {
        return (int) backgroundBounds.height();
    }

    public void setNumber(int number) {
        this.number = Math.max(0, number);
        numberText = String.valueOf(this.number);
        if (this.number > MAX_NUMBER) {
            numberText = MAX_NUMBER + EXCEED_MAX_BADGE_NUMBER_SUFFIX;
        }
        computeBadgeBounds();
        invalidateSelf();
    }

    public int getNumber() {
        return number;
    }

    public boolean hasNumber() {
        return numberText != null;
    }

    void setAnchorBounds(Rect bounds) {
        setAnchorBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
    }

    void setAnchorBounds(int left, int top, int right, int bottom) {
        Rect bounds = getBounds();
        int centerX = bounds.width() / 2;
        int centerY = bounds.height() / 2;
        int anchorWidth = right - left;
        int anchorHeight = bottom - top;

        anchorBounds.left = centerX - anchorWidth / 2;
        anchorBounds.top = centerY - anchorHeight / 2;
        anchorBounds.right = anchorBounds.left + anchorWidth;
        anchorBounds.bottom = anchorBounds.top + anchorHeight;
    }

    void computeBadgeBounds() {
        int offset = context.getResources().getDimensionPixelOffset(
                hasNumber()
                        ? R.dimen.bnb_badge_with_text_horizontal_offset_along_anchor
                        : R.dimen.bnb_badge_without_text_horizontal_offset_along_anchor);

        // If there is no number or the number is less than 9, let height equal width,
        // namely draw a circle.
        if (!hasNumber()) {
            backgroundHalfWidth = withoutTextCornerRadius;
            backgroundHalfHeight = withoutTextCornerRadius;
        } else if (number <= MAX_CIRCULAR_BADGE_NUMBER) {
            backgroundHalfWidth = withTextCornerRadius;
            backgroundHalfHeight = withTextCornerRadius;
        } else {
            float textWidth = textPaint.measureText(numberText, 0, numberText.length());

            backgroundHalfHeight = withTextCornerRadius;
            backgroundHalfWidth = textWidth / 2f + textHorizontalPadding;
        }

        // In horizontal direction, use the upper right corner of anchor as start point,
        // and offset back a bit.
        backgroundCenterX = anchorBounds.right + backgroundHalfWidth - offset;
        // In vertical direction, use the point as midpoint, no offset.
        backgroundCenterY = anchorBounds.top;

        backgroundBounds.set(
                backgroundCenterX - backgroundHalfWidth,
                backgroundCenterY - backgroundHalfHeight,
                backgroundCenterX + backgroundHalfWidth,
                backgroundCenterY + backgroundHalfHeight);
    }

    void updateBadgeBounds() {
        computeBadgeBounds();
        invalidateSelf();
    }
}
