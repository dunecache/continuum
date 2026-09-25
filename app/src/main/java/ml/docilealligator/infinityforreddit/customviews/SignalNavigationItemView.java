package ml.docilealligator.infinityforreddit.customviews;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import com.google.android.material.color.MaterialColors;
import ml.docilealligator.infinityforreddit.R;

public final class SignalNavigationItemView extends FrameLayout {
    private final LinearLayout contentView;
    private final AppCompatImageView iconView;
    private final AppCompatTextView labelView;
    private final View indicatorView;
    private boolean active;
    private boolean hasIconTint;
    private int iconTintColor;

    public SignalNavigationItemView(Context context) {
        this(context, null);
    }

    public SignalNavigationItemView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignalNavigationItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.navigation_item_min_height));
        setPadding(getResources().getDimensionPixelSize(R.dimen.space_4), 0,
                getResources().getDimensionPixelSize(R.dimen.space_4), 0);
        setClickable(true);
        setFocusable(true);
        TypedValue backgroundValue = new TypedValue();
        if (getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground,
                backgroundValue, true)) {
            setBackgroundResource(backgroundValue.resourceId);
        }

        indicatorView = new View(context);
        indicatorView.setLayoutParams(new LayoutParams(
                getResources().getDimensionPixelSize(R.dimen.navigation_indicator_width),
                getResources().getDimensionPixelSize(R.dimen.navigation_indicator_height),
                Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        GradientDrawable indicatorBackground = new GradientDrawable();
        indicatorBackground.setCornerRadius(getResources().getDimension(R.dimen.shape_corner_large));
        indicatorBackground.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimaryContainer));
        indicatorView.setBackground(indicatorBackground);
        addView(indicatorView);

        contentView = new LinearLayout(context);
        contentView.setOrientation(LinearLayout.VERTICAL);
        contentView.setGravity(Gravity.CENTER);
        contentView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(contentView);

        iconView = new AppCompatImageView(context);
        iconView.setLayoutParams(new LinearLayout.LayoutParams(
                getResources().getDimensionPixelSize(R.dimen.navigation_icon_size),
                getResources().getDimensionPixelSize(R.dimen.navigation_icon_size)));
        iconView.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        contentView.addView(iconView);

        labelView = new AppCompatTextView(context);
        labelView.setSingleLine(true);
        labelView.setEllipsize(TextUtils.TruncateAt.END);
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, resolveDimension(R.attr.font_12, 12f));
        labelView.setGravity(Gravity.CENTER);
        labelView.setIncludeFontPadding(false);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = getResources().getDimensionPixelSize(R.dimen.space_2);
        contentView.addView(labelView, labelParams);

        applyState();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // The row is a fixed height, so the label gets whatever is left after the icon rather than
        // whatever size it asks for. Left to measure itself it overflows the row at large font
        // scales, which pushed the icon out of the top (and the active pill out with it) and left
        // the row showing icons only. If a whole line will not fit, the label goes away and the
        // icon centres on its own, which is what a navigation bar is expected to do.
        int available = MeasureSpec.getSize(heightMeasureSpec)
                - getPaddingTop() - getPaddingBottom();
        int iconHeight = iconView.getLayoutParams().height;
        int labelMargin = ((LinearLayout.LayoutParams) labelView.getLayoutParams()).topMargin;
        int labelRoom = available - iconHeight - labelMargin;
        if (labelRoom > 0) {
            labelView.measure(
                    MeasureSpec.makeMeasureSpec(
                            Math.max(0, MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft()
                                    - getPaddingRight()), MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(labelRoom, MeasureSpec.AT_MOST));
        }
        labelView.setVisibility(labelRoom > 0 && labelView.getMeasuredHeight() > 0
                ? VISIBLE : GONE);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // The active indicator is a pill behind the icon, not a row above it: the row has to hold
        // the icon and its label, and spending 24dp of it on a separate indicator row left the
        // label with nowhere to go. Centring the pill on the icon is also what keeps it inside the
        // row once onMeasure has decided whether the label fits.
        int iconCentre = contentView.getTop() + iconView.getTop() + iconView.getHeight() / 2;
        int pillCentre = indicatorView.getTop() + indicatorView.getHeight() / 2;
        indicatorView.setTranslationY(iconCentre - pillCentre);
    }

    public void setImageResource(@DrawableRes int drawableResource) {
        iconView.setImageResource(drawableResource);
    }

    public void setLabel(@Nullable CharSequence label) {
        labelView.setText(label);
        setContentDescription(label);
    }

    public void setIconTint(@ColorInt int color) {
        hasIconTint = true;
        iconTintColor = color;
        applyState();
    }

    public void setActive(boolean active) {
        if (this.active == active) {
            return;
        }
        this.active = active;
        setSelected(active);
        applyState();
    }

    private void applyState() {
        indicatorView.setVisibility(active ? VISIBLE : INVISIBLE);
        int stateColor = MaterialColors.getColor(this,
                active ? com.google.android.material.R.attr.colorOnPrimaryContainer
                        : com.google.android.material.R.attr.colorOnSurfaceVariant);
        if (!active && hasIconTint) {
            // The custom theme tints the icons; leaving the labels on the theme default made them
            // the one part of the bar that ignored it.
            stateColor = iconTintColor;
        }
        labelView.setTextColor(stateColor);
        iconView.setColorFilter(stateColor, android.graphics.PorterDuff.Mode.SRC_IN);
    }

    private float resolveDimension(int attribute, float fallback) {
        TypedValue value = new TypedValue();
        if (getContext().getTheme().resolveAttribute(attribute, value, true)
                && value.type == TypedValue.TYPE_DIMENSION) {
            return value.data;
        }
        return fallback;
    }
}
