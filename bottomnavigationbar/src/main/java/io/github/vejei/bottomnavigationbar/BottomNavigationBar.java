package io.github.vejei.bottomnavigationbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Path;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.LinearLayout;
import android.widget.PopupMenu;

import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.ViewCompat;
import androidx.customview.view.AbsSavedState;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class BottomNavigationBar extends ViewGroup {
    /** Label is shown on all navigation items. */
    public static final int LABEL_VISIBILITY_ALWAYS = 0;

    /** Label is not shown on any navigation items. */
    public static final int LABEL_VISIBILITY_NEVER = 1;

    /** Label is shown on the selected navigation item. */
    public static final int LABEL_VISIBILITY_SELECTED = 2; // TODO

    /** Label is shown on the unselected navigation items. */
    public static final int LABEL_VISIBILITY_UNSELECTED = 3; // TODO

    @IntDef({LABEL_VISIBILITY_ALWAYS, LABEL_VISIBILITY_NEVER})
    @Retention(RetentionPolicy.SOURCE)
    @interface LabelVisibilityMode {}

    /** The action view will be embedded in the bar, regardless of its height. */
    public static final int ATTACH_MODE_EMBED = 0;

    /** The bar will bulge if the height of the action view exceeds it. */
    public static final int ATTACH_MODE_HUMP = 1;

    /** The action view overlaps the bar and does not impact its shape. */
    public static final int ATTACH_MODE_OVERLAP = 2;

    @IntDef({ATTACH_MODE_EMBED, ATTACH_MODE_HUMP, ATTACH_MODE_OVERLAP})
    @Retention(RetentionPolicy.SOURCE)
    @interface ActionViewAttachMode {}

    private static final String CLASS_NAME = BottomNavigationBar.class.getSimpleName();
    private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};
    private static final int[] DISABLED_STATE_SET = {-android.R.attr.state_enabled};

    private final int itemHeight;
    private final int maxHumpHeight;

    private Menu menu;
    private BottomNavigationItemView[] itemViews;
    private int selectedItemPosition = 0;

    private final ViewContainer actionViewContainer;
    private boolean hasActionView;
    private View actionView;
    @ActionViewAttachMode private int actionViewAttachMode = ATTACH_MODE_EMBED;
    private int actionViewHumpPadding;

    private boolean humpEnabled = false;
    private float humpWidth;
    private float humpHeight;

    private int itemIconSize;
    private ColorStateList itemIconTint;
    private int itemLabelTextAppearanceInactive;
    private int itemLabelTextAppearanceActive;

    private ColorStateList itemLabelTextColor;
    private final ColorStateList itemLabelTextColorDefault;

    private int itemContentSpacing;
    private int itemBackgroundRes;
    private ColorStateList itemRippleColor;
    private boolean itemRippleEnabled;
    private boolean itemUnboundedRipple;
    @LabelVisibilityMode private int itemLabelVisibilityMode;

    private int canvasSaveCount = 0;
    private final Path outlinePath = new Path();

    private OnNavigationItemSelectedListener itemSelectedListener;
    private OnNavigationItemReselectedListener itemReselectedListener;

    /**
     * The maximum number of items supported by the bar.
     * @see #getMaxItemCount()
     */
    private static final int MAX_ITEM_COUNT = 5;

    /**
     * The minimum number of items supported by the bar.
     * @see #getMinItemCount()
     */
    private static final int MIN_ITEM_COUNT = 3;

    public BottomNavigationBar(Context context) {
        this(context, null);
    }

    public BottomNavigationBar(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.bottom_navigation_bar_style);
    }

    public BottomNavigationBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.BottomNavigationBar);
    }

    public BottomNavigationBar(final Context context, AttributeSet attrs, int defStyleAttr,
                               int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setWillNotDraw(false);

        // Change the outline to canvas path, so that the background shadow follows the shape
        // of canvas.
        /*setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setConvexPath(outlinePath);
            }
        });*/

        actionViewContainer = new ViewContainer(context);

        Resources resources = getResources();
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        itemHeight = resources.getDimensionPixelSize(R.dimen.bnb_bottom_navigation_bar_height);
        maxHumpHeight = resources.getDimensionPixelOffset(
                R.dimen.bnb_bottom_navigation_bar_max_hump_height);
        itemLabelTextColorDefault = createDefaultColorStateList();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BottomNavigationBar,
                defStyleAttr, defStyleRes);

        hasActionView = a.hasValue(R.styleable.BottomNavigationBar_action_layout);
        if (hasActionView) {
            int defaultHumpPadding = resources.getDimensionPixelSize(
                    R.dimen.bnb_bottom_navigation_bar_action_view_hump_padding);

            actionView = layoutInflater.inflate(
                    a.getResourceId(R.styleable.BottomNavigationBar_action_layout, 0),
                    actionViewContainer);
            actionView.setClickable(true);
            actionViewAttachMode = a.getInteger(
                    R.styleable.BottomNavigationBar_action_view_attach_mode, ATTACH_MODE_EMBED);
            actionViewHumpPadding = a.getDimensionPixelSize(
                    R.styleable.BottomNavigationBar_action_view_hump_padding, defaultHumpPadding);
        }

        itemIconSize = a.getDimensionPixelSize(R.styleable.BottomNavigationBar_item_icon_size,
                resources.getDimensionPixelSize(R.dimen.bnb_bottom_navigation_bar_icon_size));
        if (a.hasValue(R.styleable.BottomNavigationBar_item_icon_tint)) {
            itemIconTint = a.getColorStateList(R.styleable.BottomNavigationBar_item_icon_tint);
        } else {
            itemIconTint = createDefaultColorStateList();
        }
        if (a.hasValue(R.styleable.BottomNavigationBar_item_label_text_appearance_inactive)) {
            itemLabelTextAppearanceInactive = a.getResourceId(
                    R.styleable.BottomNavigationBar_item_label_text_appearance_inactive, 0);
        }
        if (a.hasValue(R.styleable.BottomNavigationBar_item_label_text_appearance_active)) {
            itemLabelTextAppearanceActive = a.getResourceId(
                    R.styleable.BottomNavigationBar_item_label_text_appearance_active, 0);
        }
        if (a.hasValue(R.styleable.BottomNavigationBar_item_label_text_color)) {
            itemLabelTextColor = a.getColorStateList(
                    R.styleable.BottomNavigationBar_item_label_text_color);
        }
        itemContentSpacing = a.getDimensionPixelSize(
                R.styleable.BottomNavigationBar_item_content_spacing,
                resources.getDimensionPixelSize(
                        R.dimen.bnb_bottom_navigation_bar_item_content_spacing));

        itemBackgroundRes = a.getResourceId(R.styleable.BottomNavigationBar_item_background,
                0);
        if (a.hasValue(R.styleable.BottomNavigationBar_item_ripple_color)) {
            itemRippleColor = a.getColorStateList(R.styleable.BottomNavigationBar_item_ripple_color);
        }
        itemRippleEnabled = a.getBoolean(R.styleable.BottomNavigationBar_item_ripple_enabled,
                true);
        itemUnboundedRipple = a.getBoolean(R.styleable.BottomNavigationBar_item_unbounded_ripple,
                true);
        itemLabelVisibilityMode = a.getInteger(
                R.styleable.BottomNavigationBar_item_label_visibility_mode, LABEL_VISIBILITY_ALWAYS);

        if (a.hasValue(R.styleable.BottomNavigationBar_elevation)) {
            setElevation(a.getDimensionPixelSize(R.styleable.BottomNavigationBar_elevation,
                    0));
        }
        if (getBackground() == null) {
            setBackground(new ColorDrawable(Color.WHITE));
        }

        if (a.hasValue(R.styleable.BottomNavigationBar_navigation_menu)) {
            inflateMenu(a.getResourceId(R.styleable.BottomNavigationBar_navigation_menu,
                    0));
        }

        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int childCount = getChildCount();

        int itemWidth = width / (menu.hasVisibleItems() ? childCount : 1);

        // The sum of the widths of all children.
        int childrenWidth = 0;

        int itemWidthSpec = MeasureSpec.makeMeasureSpec(itemWidth, MeasureSpec.EXACTLY);
        int itemHeightSpec = MeasureSpec.makeMeasureSpec(itemHeight, MeasureSpec.EXACTLY);
        int barWidthSpec;
        int barHeightSpec;
        int barHeight = itemHeight;

        if (!hasActionView) {
            humpEnabled = false;

            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                if (child.getVisibility() == View.GONE) {
                    continue;
                }

                child.measure(itemWidthSpec, itemHeightSpec);
                childrenWidth += child.getMeasuredWidth();
            }

            barHeight = itemHeight;
            barHeightSpec = itemHeightSpec;
        } else {
            int actionViewHeight = 0;

            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                if (child.getVisibility() == View.GONE) {
                    continue;
                }

                if (child instanceof ViewContainer) {
                    int childHeightMeasureSpec;
                    ViewGroup.LayoutParams layoutParams = child.getLayoutParams();

                    if (actionViewAttachMode == ATTACH_MODE_EMBED) {
                        childHeightMeasureSpec = itemHeightSpec;
                    } else {
                        childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, 0,
                                layoutParams.height);
                    }

                    child.measure(itemWidthSpec, childHeightMeasureSpec);
                    actionViewHeight = child.getMeasuredHeight();

                    humpEnabled = ((child.getMeasuredHeight() > itemHeight)
                            && (actionViewAttachMode == ATTACH_MODE_HUMP));
                    if (humpEnabled) {
                        humpWidth = itemWidth * 1.5f;
                        humpHeight = Math.min(
                                child.getMeasuredHeight() - itemHeight + actionViewHumpPadding,
                                maxHumpHeight);
                    }
                } else {
                    child.measure(itemWidthSpec, itemHeightSpec);
                }
                childrenWidth += child.getMeasuredWidth();
            }

            switch (actionViewAttachMode) {
                case ATTACH_MODE_EMBED:
                    barHeight = itemHeight;
                    break;
                case ATTACH_MODE_HUMP:
                    barHeight = (int) (itemHeight + humpHeight);
                    break;
                case ATTACH_MODE_OVERLAP:
                    barHeight = Math.max(itemHeight, actionViewHeight);
                    break;
            }

            barHeightSpec = MeasureSpec.makeMeasureSpec(barHeight, MeasureSpec.EXACTLY);
        }

        barWidthSpec = MeasureSpec.makeMeasureSpec(childrenWidth, MeasureSpec.EXACTLY);
        setMeasuredDimension(resolveSizeAndState(childrenWidth, barWidthSpec, 0),
                resolveSizeAndState(barHeight, barHeightSpec, 0));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();
        int width = r - l;
        int height = b - t;
        int used = 0;

        for (int i = 0; i < childCount; i++) {
            int childLeft;
            int childTop;
            int childRight;
            int childBottom;

            View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                continue;
            }

            if (isLayoutLtr(this)) {
                childLeft = used;
                childRight = child.getMeasuredWidth() + used;
            } else {
                childLeft = width - used - child.getMeasuredWidth();
                childRight = width - used;
            }
            childTop = height - itemHeight;
            childBottom = height;

            if (child instanceof ViewContainer) {
                int childHeight = child.getMeasuredHeight();
                childTop = (height - childHeight) / 2;
                childBottom = childTop + childHeight;
            }

            child.layout(childLeft, childTop, childRight, childBottom);

            used += child.getMeasuredWidth();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (!humpEnabled && (actionViewAttachMode == ATTACH_MODE_OVERLAP)) {
            canvasSaveCount = canvas.save();
            outlinePath.reset();
            outlinePath.addRect(0, getHeight() - itemHeight, getWidth(), getHeight(),
                    Path.Direction.CW);
            canvas.clipPath(outlinePath);

            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRect(0, getHeight() - itemHeight, getWidth(), getHeight());
                }
            });
        } else if (humpEnabled && (actionViewAttachMode == ATTACH_MODE_HUMP)) {
            canvasSaveCount = canvas.save();
            outlinePath.reset();

            // There is hump in the center, add bezier curve to the path.
            int barWidth = getWidth();
            int barHeight = getHeight();
            float humpStartX = (barWidth - humpWidth) / 2f;
            float humpWidthQuarter = humpWidth / 4f;

            outlinePath.moveTo(0, humpHeight);
            outlinePath.lineTo(humpStartX, humpHeight);
            outlinePath.cubicTo(humpStartX + humpWidthQuarter, humpHeight,
                    humpStartX + humpWidthQuarter, 0,
                    humpStartX + humpWidth / 2f, 0);
            outlinePath.cubicTo(humpStartX + humpWidthQuarter * 3, 0,
                    humpStartX + humpWidthQuarter * 3, humpHeight,
                    humpStartX + humpWidth, humpHeight);
            outlinePath.lineTo(barWidth, humpHeight);
            outlinePath.lineTo(barWidth, barHeight);
            outlinePath.lineTo(0, barHeight);

            canvas.clipPath(outlinePath);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                setOutlineProvider(null);
            } else {
                setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        outline.setConvexPath(outlinePath);
                    }
                });
            }
        }

        super.draw(canvas);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (canvasSaveCount != 0) {
            canvas.restoreToCount(canvasSaveCount);
        }
    }

    @Override
    public void addView(View child) {
        validateChildView(child);
        super.addView(child);
    }

    @Override
    public void addView(View child, int index) {
        validateChildView(child);
        super.addView(child, index);
    }

    @Override
    public void addView(View child, int index, LayoutParams params) {
        validateChildView(child);
        super.addView(child, index, params);
    }

    @Override
    public void addView(View child, LayoutParams params) {
        validateChildView(child);
        super.addView(child, params);
    }

    @Override
    public void addView(View child, int width, int height) {
        validateChildView(child);
        super.addView(child, width, height);
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.selectedItemPosition = selectedItemPosition;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        this.selectedItemPosition = savedState.selectedItemPosition;
        updateNavigationItems();
    }

    private void validateChildView(View child) {
        boolean isUnknown = !(child instanceof BottomNavigationItemView)
                && !(child instanceof ViewContainer)
                && !(child instanceof PlaceholderView);
        if (isUnknown) {
            throw new IllegalArgumentException("Unknown child view.");
        }
    }

    @Nullable
    private ColorStateList createDefaultColorStateList() {
        final TypedValue value = new TypedValue();
        if (!getContext().getTheme().resolveAttribute(android.R.attr.textColorSecondary, value,
                true)) {
            return null;
        }
        ColorStateList baseColor = AppCompatResources.getColorStateList(getContext(),
                value.resourceId);
        if (!getContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary,
                value, true)) {
            return null;
        }
        int colorPrimary = value.data;
        int defaultColor = baseColor.getDefaultColor();
        return new ColorStateList(
                new int[][] {DISABLED_STATE_SET, CHECKED_STATE_SET, EMPTY_STATE_SET},
                new int[] {
                        baseColor.getColorForState(DISABLED_STATE_SET, defaultColor), colorPrimary,
                        defaultColor
                });
    }

    private void inflateMenu(@MenuRes int menuRes) {
        PopupMenu popupMenu = new PopupMenu(getContext(), null);
        this.menu = popupMenu.getMenu();
        popupMenu.getMenuInflater().inflate(menuRes, this.menu);

        // Check whether the menu is valid, throw exception if the menu is invalid.
        validateMenu();

        buildNavigationItems();
    }

    private void validateMenu() {
        if (menu == null) {
            return;
        }

        int menuSize = this.menu.size();
        if ((hasActionView && ((menuSize + 1) > MAX_ITEM_COUNT))
                || (!hasActionView && (menuSize > MAX_ITEM_COUNT))) {
            throw new IllegalArgumentException("Maximum number of items supported by " + CLASS_NAME
                    + " is " + MAX_ITEM_COUNT + ". Using " + CLASS_NAME
                    + "#getMaxItemCount to check the maximum item count.");
        }
        if ((hasActionView && (menuSize + 1 < MIN_ITEM_COUNT))
                || (!hasActionView && (menuSize < MIN_ITEM_COUNT))) {
            throw new IllegalArgumentException("Minimum number of items supported by " + CLASS_NAME
                    + " is " + MIN_ITEM_COUNT + ". Using " + CLASS_NAME
                    + "#getMinItemCount to check the minimum item count.");
        }

        for (int i = 0; i < menuSize; i++) {
            MenuItem menuItem = menu.getItem(i);
            if (menuItem.hasSubMenu()) {
                throw new UnsupportedOperationException(CLASS_NAME + " does not support submenus.");
            }
        }
    }

    private void buildNavigationItems() {
        removeAllViews();

        itemViews = new BottomNavigationItemView[menu.size()];
        if (hasActionView) {
            int itemCount = computeItemCount();
            int centerPosition = itemCount / 2;
            int leftEndPosition = centerPosition - 1;
            int rightStartPosition = centerPosition + 1;
            int leftItemCount = leftEndPosition + 1;
            int rightItemCount = itemCount - rightStartPosition;

            // Build the left navigation items.
            for (int i = 0; i <= leftEndPosition; i++) {
                buildAndAddItemView(i);
            }

            // Add action view to center
            addView(actionViewContainer, centerPosition);

            // Build the right navigation items.
            for (int i = rightStartPosition; i < itemCount; i++) {
                int position = i - 1;

                MenuItem menuItem = menu.getItem(position);
                menuItem.setCheckable(true);
                menuItem.setChecked(position == selectedItemPosition);

                BottomNavigationItemView itemView = buildItemView(menuItem, position);
                itemViews[position] = itemView;
                addView(itemView, i);
            }

            // If the number of navigation items on the right is less than that on the left,
            // add placeholder.
            if (rightItemCount != leftItemCount) {
                addView(new PlaceholderView(getContext()));
            }
        } else {
            // If action layout disabled, build navigation items only.
            for (int i = 0; i < menu.size(); i++) {
                buildAndAddItemView(i);
            }
        }
    }

    private void buildAndAddItemView(int position) {
        MenuItem menuItem = menu.getItem(position);
        menuItem.setCheckable(true);
        menuItem.setChecked(position == selectedItemPosition);

        BottomNavigationItemView itemView = buildItemView(menuItem, position);
        itemViews[position] = itemView;
        addView(itemView, position);
    }

    private BottomNavigationItemView buildItemView(final MenuItem item, int position) {
        final BottomNavigationItemView itemView = new BottomNavigationItemView(getContext());

        itemView.setPosition(position);
        itemView.setIconSize(itemIconSize);
        itemView.setIconTint(itemIconTint);
        itemView.setLabelTextColor(itemLabelTextColorDefault);
        itemView.setLabelTextAppearanceInactive(itemLabelTextAppearanceInactive);
        itemView.setLabelTextAppearanceActive(itemLabelTextAppearanceActive);
        itemView.setLabelTextColor(itemLabelTextColor);
        itemView.setContentSpacing(itemContentSpacing);
        itemView.setRippleEnabled(itemRippleEnabled);
        itemView.setUnboundedRipple(itemUnboundedRipple);

        if (itemBackgroundRes != 0) {
            itemView.setViewBackground(itemBackgroundRes);
        } else {
            itemView.setRippleBackground(itemRippleColor);
        }

        itemView.setLabelVisibilityMode(itemLabelVisibilityMode);

        itemView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean reselected = (selectedItemPosition == itemView.getPosition());

                if (itemSelectedListener != null) {
                    itemSelectedListener.onNavigationItemSelected(item);
                }
                if (reselected && (itemReselectedListener != null)) {
                    itemReselectedListener.onNavigationItemReselected(item);
                }
                selectedItemPosition = itemView.getPosition();
                updateNavigationItems();
            }
        });

        itemView.updateView(item);
        return itemView;
    }

    private int computeItemCount() {
        return !hasActionView ? menu.size() : menu.size() + 1;
    }

    private void updateNavigationItems() {
        if (menu == null || itemViews == null) {
            return;
        }

        int menuSize = menu.size();
        if (itemViews.length != menuSize) {
            buildNavigationItems();
        }

        for (int i = 0; i < menuSize; i++) {
            boolean selected = (i == selectedItemPosition);

            MenuItem menuItem = menu.getItem(i);
            menuItem.setChecked(selected);
            itemViews[i].updateView(menuItem);
        }
    }

    private boolean isLayoutLtr(View view) {
        return ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_LTR;
    }

    /**
     * Return the maximum number of items that can be shown in bar, including the middle action view,
     * if enabled.
     *
     * @return the maximum number of items that can be shown in bar.
     */
    public int getMaxItemCount() {
        return MAX_ITEM_COUNT;
    }

    /**
     * Return the minimum number of items that can be shown in bar, including the middle action view,
     * if enabled.
     *
     * @return the minimum number of items that can be shown in bar.
     */
    public int getMinItemCount() {
        return MIN_ITEM_COUNT;
    }

    /** 
     * @return the current action view 
     * @see #setActionView(View) 
     */
    public View getActionView() {
        return actionView;
    }

    /**
     * Set the view which act as the center action view.
     * 
     * @param actionView the center action view.
     */
    public void setActionView(View actionView) {
        hasActionView = (actionView != null);
        this.actionView = actionView;
        requestLayout();
    }

    /**
     * Get the current action view attach mode.
     *
     * @return one of {@link #ATTACH_MODE_EMBED}, {@link #ATTACH_MODE_HUMP} or
     * {@link #ATTACH_MODE_OVERLAP}
     * @see #setActionViewAttachMode(int)
     */
    @ActionViewAttachMode
    public int getActionViewAttachMode() {
        return actionViewAttachMode;
    }

    /**
     * Set the attach mode of the action view, which is used to determine how the action view
     * attaches to the bar(embed, hump or overlap).
     *
     * @param actionViewAttachMode one of {@link #ATTACH_MODE_EMBED}, {@link #ATTACH_MODE_HUMP} or
     * {@link #ATTACH_MODE_OVERLAP}
     */
    public void setActionViewAttachMode(@ActionViewAttachMode int actionViewAttachMode) {
        if (this.actionViewAttachMode != actionViewAttachMode) {
            this.actionViewAttachMode = actionViewAttachMode;
            requestLayout();
        }
    }

    /**
     * @return the padding between hump and action view.
     * @see #setActionViewHumpPadding(int)
     */
    @Dimension
    public int getActionViewHumpPadding() {
        return actionViewHumpPadding;
    }

    /**
     * Set the padding for hump.
     *
     * @param padding the padding between hump and action view.
     */
    public void setActionViewHumpPadding(@Dimension int padding) {
        if (this.actionViewHumpPadding != padding) {
            this.actionViewHumpPadding = padding;
            requestLayout();
        }
    }

    /**
     * @return the current icon size of menu item.
     * @see #setItemIconSize(int)
     */
    @Dimension
    public int getItemIconSize() {
        return itemIconSize;
    }

    /**
     * Set the size to provide for the menu item icons.
     *
     * @param itemIconSize the size in pixels to provide for the menu item icons
     */
    public void setItemIconSize(@Dimension int itemIconSize) {
        this.itemIconSize = itemIconSize;
        if (itemViews != null) {
            for (BottomNavigationItemView itemView : itemViews) {
                itemView.setIconSize(itemIconSize);
            }
        }
    }

    /**
     * @return the tint which is applied to menu items' icons.
     * @see #setItemIconTintList(ColorStateList)
     */
    public ColorStateList getItemIconTintList() {
        return itemIconTint;
    }

    /**
     * Set the tint which is applied to menu items' icons.
     *
     * @param tint the tint to apply.
     */
    public void setItemIconTintList(ColorStateList tint) {
        this.itemIconTint = tint;
        if (itemViews != null) {
            for (BottomNavigationItemView itemView : itemViews) {
                itemView.setIconTint(itemIconTint);
            }
        }
    }

    /**
     * Get the text appearance used for inactive menu item labels.
     *
     * @return the text appearance ID used for inactive menu item labels
     */
    @StyleRes
    public int getItemLabelTextAppearanceInactive() {
        return itemLabelTextAppearanceInactive;
    }

    /**
     * Sets the text appearance to be used for inactive menu item labels.
     *
     * @param textAppearanceRes the text appearance ID used for inactive menu item labels
     */
    public void setItemLabelTextAppearanceInactive(@StyleRes int textAppearanceRes) {
        this.itemLabelTextAppearanceInactive = textAppearanceRes;
        if (itemViews != null) {
            for (BottomNavigationItemView itemView : itemViews) {
                itemView.setLabelTextAppearanceInactive(textAppearanceRes);
                if (itemLabelTextColor != null) {
                    itemView.setLabelTextColor(itemLabelTextColor);
                }
            }
        }
    }

    /**
     * Get the text appearance used for the active menu item label.
     *
     * @return the text appearance ID used for the active menu item label
     */
    @StyleRes
    public int getItemLabelTextAppearanceActive() {
        return itemLabelTextAppearanceActive;
    }

    /**
     * Sets the text appearance to be used for active menu item labels.
     *
     * @param textAppearanceRes the text appearance ID used for active menu item labels
     */
    public void setItemLabelTextAppearanceActive(@StyleRes int textAppearanceRes) {
        this.itemLabelTextAppearanceActive = textAppearanceRes;
        if (itemViews != null) {
            for (BottomNavigationItemView itemView : itemViews) {
                itemView.setLabelTextAppearanceActive(textAppearanceRes);
                if (itemLabelTextColor != null) {
                    itemView.setLabelTextColor(itemLabelTextColor);
                }
            }
        }
    }

    /**
     * Get colors used for the different states (normal, selected, focused, etc.) of the menu item
     * text.
     *
     * @return the ColorStateList of colors used for the different states of the menu items text.
     */
    public ColorStateList getItemLabelTextColor() {
        return itemLabelTextColor;
    }

    /**
     * Set the colors to use for the different states (normal, selected, focused, etc.) of the menu
     * item text.
     *
     * @param color the {@link ColorStateList} for the menu item text.
     */
    public void setItemLabelTextColor(ColorStateList color) {
        this.itemLabelTextColor = color;
        if (itemViews != null) {
            for (BottomNavigationItemView itemView : itemViews) {
                itemView.setLabelTextColor(color);
            }
        }
    }

    /**
     * Get the spacing between item's icon and item's label.
     *
     * @return the current spacing in pixels
     */
    @Dimension
    public int getItemContentSpacing() {
        return itemContentSpacing;
    }

    /**
     * Set the spacing between icon and label for navigation item.
     *
     * @param itemContentSpacing the spacing in pixels to apply to navigation item
     */
    public void setItemContentSpacing(@Dimension int itemContentSpacing) {
        this.itemContentSpacing = itemContentSpacing;
        if (itemViews != null) {
            for (BottomNavigationItemView itemView : itemViews) {
                itemView.setContentSpacing(itemContentSpacing);
            }
        }
    }

    /**
     * @return the background resource of menu item.
     */
    @DrawableRes
    public int getItemBackgroundRes() {
        return itemBackgroundRes;
    }

    /**
     * Set the background of menu items to the given resource.
     *
     * @param backgroundRes the identifier of background drawable resource
     */
    public void setItemBackgroundRes(@DrawableRes int backgroundRes) {
        this.itemBackgroundRes = backgroundRes;
        if (itemViews != null) {
            for (BottomNavigationItemView itemView : itemViews) {
                itemView.setViewBackground(itemBackgroundRes);
            }
        }
    }

    /**
     * @return the current ripple color which used as navigation's background
     */
    public ColorStateList getItemRippleColor() {
        return itemRippleColor;
    }

    /**
     * Set the background of menu items to be a ripple with the given colors.
     *
     * @param color the {@link ColorStateList} for the ripple.
     */
    public void setItemRippleColor(ColorStateList color) {
        this.itemRippleColor = color;
        if (itemViews != null) {
            for (BottomNavigationItemView itemView : itemViews) {
                itemView.setRippleColor(color);
            }
        }
    }

    /**
     * Check whether the current navigation item ripple enabled.
     *
     * @return true if enabled, false otherwise.
     */
    public boolean isItemRippleEnabled() {
        return itemRippleEnabled;
    }

    /**
     * Set whether the current navigation item ripple enabled.
     *
     * @param enabled whether the navigation item enable ripple
     */
    public void setItemRippleEnabled(boolean enabled) {
        this.itemRippleEnabled = enabled;
        if (itemViews != null) {
            for (BottomNavigationItemView itemView : itemViews) {
                itemView.setRippleEnabled(enabled);
            }
        }
    }

    /**
     * @return true is unbounded, false otherwise
     */
    public boolean isItemUnboundedRipple() {
        return itemUnboundedRipple;
    }

    /**
     * Set whether navigation item will have an unbounded ripple effect or if ripple will be
     * bound to the tab item size.
     *
     * @param itemUnboundedRipple whether use unbounded ripple
     */
    public void setItemUnboundedRipple(boolean itemUnboundedRipple) {
        this.itemUnboundedRipple = itemUnboundedRipple;
        if (itemViews != null) {
            for (BottomNavigationItemView itemView : itemViews) {
                itemView.setUnboundedRipple(itemUnboundedRipple);
            }
        }
    }

    /**
     * @return one of {@link #LABEL_VISIBILITY_ALWAYS} or {@link #LABEL_VISIBILITY_NEVER}
     */
    @LabelVisibilityMode
    public int getItemLabelVisibilityMode() {
        return itemLabelVisibilityMode;
    }

    /**
     * Sets the navigation items' label visibility mode.
     *
     * @param itemLabelVisibilityMode one of {@link #LABEL_VISIBILITY_ALWAYS}
     *                                or {@link #LABEL_VISIBILITY_NEVER}
     */
    public void setItemLabelVisibilityMode(@LabelVisibilityMode int itemLabelVisibilityMode) {
        this.itemLabelVisibilityMode = itemLabelVisibilityMode;
        if (itemViews != null) {
            for (BottomNavigationItemView itemView : itemViews) {
                itemView.setLabelVisibilityMode(itemLabelVisibilityMode);
            }
        }
    }

    /**
     * Set a listener that will be notified when a bottom navigation item is selected.
     *
     * @param listener the listener to notify
     * @see #setOnNavigationItemReselectedListener(OnNavigationItemReselectedListener) 
     */
    public void setOnNavigationItemSelectedListener(OnNavigationItemSelectedListener listener) {
        itemSelectedListener = listener;
    }

    /**
     * Set a listener that will be notified when the currently selected bottom navigation item is
     * reselected.
     *
     * @param listener the listener to notify
     * @see #setOnNavigationItemSelectedListener(OnNavigationItemSelectedListener) 
     */
    public void setOnNavigationItemReselectedListener(OnNavigationItemReselectedListener listener) {
        itemReselectedListener = listener;
    }

    public BadgeDrawable getOrCreateBadge(int menuItemId) {
        validateMenuItemId(menuItemId);
        BottomNavigationItemView itemView = findItemView(menuItemId);
        BadgeDrawable badgeDrawable = new BadgeDrawable(getContext());
        itemView.setBadge(badgeDrawable);
        return badgeDrawable;
    }

    public void removeBadge(int menuItemId) {
        validateMenuItemId(menuItemId);
        BottomNavigationItemView itemView = findItemView(menuItemId);
        itemView.removeBadge();
    }

    private BottomNavigationItemView findItemView(int menuItemId) {
        if (itemViews != null) {
            for (BottomNavigationItemView itemView : itemViews) {
                if (itemView.getId() == menuItemId) {
                    return itemView;
                }
            }
        }
        return null;
    }

    private boolean isValidId(int viewId) {
        return viewId != View.NO_ID;
    }

    private void validateMenuItemId(int viewId) {
        if (!isValidId(viewId)) {
            throw new IllegalArgumentException(viewId + " is not a valid view id");
        }
    }

    /** Listener for handling selection events on bottom navigation items. */
    public interface OnNavigationItemSelectedListener {

        /**
         * Called when an item in the bottom navigation menu is selected.
         *
         * @param item the selected item.
         */
        void onNavigationItemSelected(MenuItem item);
    }

    /** Listener for handling reselection events on bottom navigation items. */
    public interface OnNavigationItemReselectedListener {

        /**
         * Called when the currently selected item in the bottom navigation menu is selected again.
         *
         * @param item the selected item.
         */
        void onNavigationItemReselected(MenuItem item);
    }

    static class ViewContainer extends LinearLayout {
        ViewContainer(@NonNull Context context) {
            super(context);
            setGravity(Gravity.CENTER);
        }
    }

    static class PlaceholderView extends View {
        PlaceholderView(Context context) {
            super(context);
        }
    }

    static class SavedState extends AbsSavedState {
        int selectedItemPosition;

        public SavedState(@NonNull Parcelable superState) {
            super(superState);
        }

        protected SavedState(Parcel source, ClassLoader loader) {
            super(source, loader);
            selectedItemPosition = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(selectedItemPosition);
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
