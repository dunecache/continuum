package ml.docilealligator.infinityforreddit.customviews;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import com.google.android.material.color.MaterialColors;
import ml.docilealligator.infinityforreddit.R;

public final class SignalNavigationItemView extends LinearLayout {
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
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.navigation_item_min_height));
        setPadding(getResources().getDimensionPixelSize(R.dimen.space_4), 0,
                getResources().getDimensionPixelSize(R.dimen.space_4), 0);
        setClickable(true);
        setFocusable(true);
        TypedValue backgroundValue = new TypedValue();
        if (getContext().getTheme().resolveAttribute(
                com.google.android.material.R.attr.selectableItemBackgroundBorderless, backgroundValue, true)) {
            setBackground(backgroundValue.resourceId);
        }

        indicatorView = new View(context);
        indicatorView.setLayoutParams(new LayoutParams(
                getResources().getDimensionPixelSize(R.dimen.navigation_indicator_width),
                getResources().getDimensionPixelSize(R.dimen.navigation_indicator_height)));
        GradientDrawable indicatorBackground = new GradientDrawable();
        indicatorBackground.setCornerRadius(getResources().getDimension(R.dimen.shape_corner_medium));
        indicatorBackground.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimaryContainer));
        indicatorView.setBackground(indicatorBackground);
        addView(indicatorView);

        iconView = new AppCompatImageView(context);
        iconView.setLayoutParams(new LayoutParams(
                getResources().getDimensionPixelSize(R.dimen.navigation_icon_size),
                getResources().getDimensionPixelSize(R.dimen.navigation_icon_size)));
        iconView.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        addView(iconView);

        labelView = new AppCompatTextView(context);
        labelView.setSingleLine(true);
        labelView.setEllipsize(TextUtils.TruncateAt.END);
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, resolveDimension(R.attr.font_12, 12f));
        labelView.setGravity(Gravity.CENTER);
        labelView.setIncludeFontPadding(false);
        LayoutParams labelParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = getResources().getDimensionPixelSize(R.dimen.space_2);
        addView(labelView, labelParams);

        applyState();
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
        int labelColor = MaterialColors.getColor(this,
                active ? com.google.android.material.R.attr.colorOnPrimaryContainer
                        : com.google.android.material.R.attr.colorOnSurfaceVariant);
        labelView.setTextColor(labelColor);
        if (active) {
            iconView.setColorFilter(MaterialColors.getColor(this,
                    com.google.android.material.R.attr.colorOnPrimaryContainer), android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (hasIconTint) {
            iconView.setColorFilter(iconTintColor, android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            iconView.setColorFilter(labelColor, android.graphics.PorterDuff.Mode.SRC_IN);
        }
    }

    private float resolveDimension(int attribute, float fallback) {
        TypedValue value = new TypedValue();
        if (getContext().getTheme().resolveAttribute(attribute, value, true)
                && value.type >= TypedValue.TYPE_FIRST_DIMENSION
                && value.type <= TypedValue.TYPE_LAST_DIMENSION) {
            return value.data;
        }
        return fallback;
    }
}
