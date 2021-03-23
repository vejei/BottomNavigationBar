package io.github.vejei.bottomnavigationbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextPaint;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.customview.view.AbsSavedState;

import static io.github.vejei.bottomnavigationbar.BottomNavigationBar.LABEL_VISIBILITY_ALWAYS;

final class BottomNavigationItemView extends View {
    private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};

    private MenuItem menuItem;
    private boolean checked = false;

    private int position;
    private Drawable icon;
    private CharSequence label;
    private int iconSize;
    private ColorStateList iconTint;
    private ColorStateList labelTextColor;
    private int contentSpacing;
    private boolean rippleEnabled;
    private ColorStateList rippleColor;
    private boolean unboundedRipple;
    @BottomNavigationBar.LabelVisibilityMode private int labelVisibilityMode;

    private final Rect labelBounds = new Rect();
    private int labelBaselineX;
    private int labelBaselineY;

    private TextPaint textPaint;
    private TextPaint activeTextPaint;
    private TextPaint inactiveTextPaint;
    private ColorStateList activeTextColor;
    private ColorStateList inactiveTextColor;

    private BadgeDrawable badgeDrawable;

    public BottomNavigationItemView(Context context) {
        super(context);

        activeTextPaint = new TextPaint();
        inactiveTextPaint = new TextPaint();
        textPaint = inactiveTextPaint;

        setFocusable(true);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (menuItem != null && menuItem.isCheckable() && menuItem.isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (((labelVisibilityMode == LABEL_VISIBILITY_ALWAYS) && (label == null))
                || (icon == null)) {
            return;
        }

        int width = right - left;
        int height = bottom - top;
        int iconLeft;
        int iconTop;
        int iconRight;
        int iconBottom;
        int contentHeight = 0;

        switch (labelVisibilityMode) {
            case LABEL_VISIBILITY_ALWAYS:
                labelBounds.setEmpty();
                textPaint.getTextBounds(label.toString(), 0, label.length(), labelBounds);
                contentHeight = icon.getIntrinsicHeight() + (int) Math.abs(textPaint.ascent())
                        + contentSpacing;
                iconLeft = (width - icon.getIntrinsicWidth()) / 2;
                iconTop = (height - contentHeight) / 2;
                iconRight = iconLeft + icon.getIntrinsicWidth();
                iconBottom = iconTop + icon.getIntrinsicHeight();

                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

                labelBaselineX = getWidth() / 2;
                labelBaselineY = iconBottom + contentSpacing - labelBounds.top;
                break;
            case BottomNavigationBar.LABEL_VISIBILITY_NEVER:
                contentHeight = icon.getIntrinsicHeight();

                iconLeft = (width - icon.getIntrinsicWidth()) / 2;
                iconTop = (height - icon.getIntrinsicHeight()) / 2;
                iconRight = iconLeft + icon.getIntrinsicWidth();
                iconBottom = iconTop + icon.getIntrinsicHeight();

                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                break;
        }

        if (hasBadge()) {
            badgeDrawable.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
            badgeDrawable.setAnchorBounds(0, 0, icon.getIntrinsicWidth(), contentHeight);
            badgeDrawable.updateBadgeBounds();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        switch (labelVisibilityMode) {
            case LABEL_VISIBILITY_ALWAYS:
                icon.draw(canvas);
                canvas.drawText(label.toString(), labelBaselineX, labelBaselineY, textPaint);
                break;
            case BottomNavigationBar.LABEL_VISIBILITY_NEVER:
                icon.draw(canvas);
                break;
        }

        if (hasBadge()) {
            badgeDrawable.draw(canvas);
        }
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return super.verifyDrawable(who) || (who == icon)
                || (badgeDrawable != null && who == badgeDrawable);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        if (icon != null && icon.isStateful()) {
            icon.setState(getDrawableState());
        }
        int newColor = labelTextColor.getColorForState(getDrawableState(), 0);
        if (textPaint != null && newColor != textPaint.getColor()) {
            textPaint.setColor(newColor);
        }

        textPaint = checked ? activeTextPaint : inactiveTextPaint;

        invalidate();
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        if (hasBadge()) {
            Parcelable superState = super.onSaveInstanceState();
            SavedState savedState = new SavedState(superState);
            savedState.badgeNumber = badgeDrawable.getNumber();
            return savedState;
        }
        return super.onSaveInstanceState();
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        BadgeDrawable badgeDrawable = new BadgeDrawable(getContext());
        badgeDrawable.setNumber(savedState.badgeNumber);
        setBadge(badgeDrawable);
    }

    void updateView(MenuItem menuItem) {
        this.menuItem = menuItem;

        checked = menuItem.isChecked();
        if (labelTextColor == null) {
            labelTextColor = checked ? activeTextColor : inactiveTextColor;
        }

        setSelected(menuItem.isChecked());

        setEnabled(menuItem.isEnabled());
        setIcon(menuItem.getIcon());
        setLabel(menuItem.getTitle());
        setId(menuItem.getItemId());

        setVisibility(menuItem.isVisible() ? View.VISIBLE : View.GONE);

        refreshDrawableState();
    }

    public int getPosition() {
        return position;
    }

    void setPosition(int position) {
        this.position = position;
    }

    void setIcon(Drawable newIcon) {
        if (icon == newIcon) {
            return;
        }

        icon = newIcon;
        if (newIcon != null) {
            Drawable.ConstantState state = newIcon.getConstantState();
            icon = DrawableCompat.wrap(state == null ? newIcon : state.newDrawable().mutate());
            if (iconTint != null) {
                DrawableCompat.setTintList(icon, iconTint);
            }
        }

        requestLayout();
    }

    void setLabel(CharSequence label) {
        if (this.label != null && this.label.equals(label)) {
            return;
        }
        this.label = label;
        requestLayout();
    }

    void setIconSize(@Dimension int iconSize) {
        if (this.iconSize == iconSize) {
            return;
        }
        this.iconSize = iconSize;
        requestLayout();
    }

    void setIconTint(ColorStateList iconTint) {
        this.iconTint = iconTint;
        if (icon != null) {
            DrawableCompat.setTintList(icon, iconTint);
        }
        invalidate();
    }

    void setLabelTextAppearanceInactive(@StyleRes int textAppearance) {
        TypedArray appearance = getContext().getTheme().obtainStyledAttributes(textAppearance,
                androidx.appcompat.R.styleable.TextAppearance);
        inactiveTextColor = appearance.getColorStateList(
                androidx.appcompat.R.styleable.TextAppearance_android_textColor);
        appearance.recycle();

        AppCompatTextView utilTextView = new AppCompatTextView(getContext());
        utilTextView.setTextAppearance(getContext(), textAppearance);
        inactiveTextPaint = utilTextView.getPaint();
        inactiveTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    void setLabelTextAppearanceActive(@StyleRes int textAppearance) {
        TypedArray appearance = getContext().getTheme().obtainStyledAttributes(textAppearance,
                androidx.appcompat.R.styleable.TextAppearance);
        activeTextColor = appearance.getColorStateList(
                androidx.appcompat.R.styleable.TextAppearance_android_textColor);
        appearance.recycle();

        AppCompatTextView utilTextView = new AppCompatTextView(getContext());
        utilTextView.setTextAppearance(getContext(), textAppearance);
        activeTextPaint = utilTextView.getPaint();
        activeTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    void setLabelTextColor(ColorStateList color) {
        if (color != null) {
            this.labelTextColor = color;

            int newColor = labelTextColor.getColorForState(getDrawableState(), 0);
            if (textPaint != null && newColor != textPaint.getColor()) {
                textPaint.setColor(newColor);
                invalidate();
            }
        }
    }

    void setContentSpacing(@Dimension int spacing) {
        if (contentSpacing == spacing) {
            return;
        }
        this.contentSpacing = spacing;
        if (labelVisibilityMode == LABEL_VISIBILITY_ALWAYS) {
            requestLayout();
        }
    }

    void setViewBackground(@DrawableRes int backgroundRes) {
        Drawable backgroundDrawable =
                backgroundRes == 0 ? null : ContextCompat.getDrawable(getContext(), backgroundRes);
        setViewBackground(backgroundDrawable);
    }

    void setViewBackground(Drawable background) {
        if (background != null && background.getConstantState() != null) {
            background = background.getConstantState().newDrawable().mutate();
        }
        ViewCompat.setBackground(this, background);
    }

    void setRippleBackground(ColorStateList rippleColor) {
        if (this.rippleColor == rippleColor) {
            if (rippleColor == null && getBackground() != null) {
                setViewBackground(null);
            }
            return;
        }
        this.rippleColor = rippleColor;
        if (rippleEnabled) {
            ColorStateList rippleDrawableColor =
                    RippleUtils.convertToRippleDrawableColor(rippleColor);

            GradientDrawable contentDrawable = new GradientDrawable();
            contentDrawable.setColor(Color.TRANSPARENT);

            GradientDrawable maskDrawable = new GradientDrawable();
            maskDrawable.setCornerRadius(0.00001F);
            maskDrawable.setColor(Color.WHITE);

            Drawable background = new RippleDrawable(rippleDrawableColor,
                    unboundedRipple ? null : contentDrawable, unboundedRipple ? null : maskDrawable);
            setViewBackground(background);
        } else {
            setViewBackground(null);
        }
    }

    void setRippleEnabled(boolean enabled) {
        this.rippleEnabled = enabled;
        setRippleColor(rippleColor);
    }

    void setRippleColor(ColorStateList rippleColor) {
        if (this.rippleColor == rippleColor) {
            if (rippleColor == null && getBackground() != null) {
                setViewBackground(null);
            }
            return;
        }
        this.rippleColor = rippleColor;
        if (rippleEnabled) {
            ColorStateList rippleDrawableColor =
                    RippleUtils.convertToRippleDrawableColor(rippleColor);

            GradientDrawable contentDrawable = new GradientDrawable();
            contentDrawable.setColor(Color.TRANSPARENT);

            GradientDrawable maskDrawable = new GradientDrawable();
            maskDrawable.setCornerRadius(0.00001F);
            maskDrawable.setColor(Color.WHITE);

            Drawable background = new RippleDrawable(rippleDrawableColor,
                    unboundedRipple ? null : contentDrawable, unboundedRipple ? null : maskDrawable);
            setViewBackground(background);
        } else {
            setViewBackground(null);
        }
    }

    void setUnboundedRipple(boolean unbounded) {
        unboundedRipple = unbounded;
        setRippleColor(rippleColor);
    }

    void setLabelVisibilityMode(@BottomNavigationBar.LabelVisibilityMode int mode) {
        if (this.labelVisibilityMode == mode) {
            return;
        }
        this.labelVisibilityMode = mode;
        requestLayout();
    }

    void setBadge(@NonNull BadgeDrawable drawable) {
        badgeDrawable = drawable;
        badgeDrawable.setCallback(this);
        requestLayout();
    }

    BadgeDrawable getBadge() {
        return this.badgeDrawable;
    }

    boolean hasBadge() {
        return badgeDrawable != null;
    }

    void removeBadge() {
        badgeDrawable = null;
        invalidate();
    }

    static class SavedState extends AbsSavedState {
        int badgeNumber;

        public SavedState(@NonNull Parcelable superState) {
            super(superState);
        }

        protected SavedState(@NonNull Parcel source, @Nullable ClassLoader loader) {
            super(source, loader);
            badgeNumber = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(badgeNumber);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new ClassLoaderCreator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel source, ClassLoader loader) {
                        return new SavedState(source, loader);
                    }

                    @Override
                    public SavedState createFromParcel(Parcel source) {
                        return new SavedState(source, null);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
