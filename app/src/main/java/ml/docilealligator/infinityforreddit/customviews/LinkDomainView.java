package ml.docilealligator.infinityforreddit.customviews;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import com.google.android.material.color.MaterialColors;
import ml.docilealligator.infinityforreddit.R;

/**
 * The domain of a link post, as a quiet tonal chip instead of a bare line of text.
 *
 * <p>Phase 1 of docs/REDESIGN.md asks for a rounded domain chip rather than a raw URL string. The
 * spec also mentions a favicon; nothing in the app fetches one and adding a network round trip per
 * link post is deliberately out of scope here, so the chip leads with the app's link glyph and is
 * ready to take a favicon later without changing any call site.
 *
 * <p>Adapters bind these through {@code setText}, so the host is normalised here rather than at
 * every call site: {@code https://www.Example.com/path} reads as {@code example.com}.
 */
public final class LinkDomainView extends AppCompatTextView {

    public LinkDomainView(Context context) {
        this(context, null);
    }

    public LinkDomainView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LinkDomainView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setSingleLine(true);
        setEllipsize(TextUtils.TruncateAt.END);
        setGravity(Gravity.CENTER_VERTICAL);
        setIncludeFontPadding(false);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, resolveDimension(R.attr.font_12, 12f));
        setMinHeight(getResources().getDimensionPixelSize(R.dimen.domain_chip_min_height));
        int horizontalPadding = getResources().getDimensionPixelSize(R.dimen.domain_chip_padding_horizontal);
        setPadding(horizontalPadding, 0, horizontalPadding, 0);

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(getResources().getDimension(R.dimen.shape_corner_small));
        background.setColor(MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorSurfaceContainerHigh));
        setBackground(background);

        setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_link_day_night_24dp, 0, 0, 0);
        int iconSize = getResources().getDimensionPixelSize(R.dimen.domain_chip_icon_size);
        setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.space_8));
        setTextColor(MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnSurfaceVariant));
        for (int i = 0; i < getCompoundDrawables().length; i++) {
            if (getCompoundDrawables()[i] != null) {
                getCompoundDrawables()[i].setBounds(0, 0, iconSize, iconSize);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Normalises the host and hides the chip when there is nothing to show, so a recycled row
     * cannot keep the domain of the post it used to hold.
     */
    @Override
    public void setText(CharSequence text, BufferType type) {
        CharSequence host = normalizeHost(text);
        super.setText(host, type);
        setVisibility(host.length() == 0 ? GONE : VISIBLE);
    }

    @Override
    public void setTextColor(int color) {
        super.setTextColor(color);
        // Whatever the theme hands the chip's text, the glyph follows it, so a custom theme that
        // recolours the secondary text does not leave a mismatched icon behind.
        setCompoundDrawableTintList(ColorStateList.valueOf(color));
    }

    private static CharSequence normalizeHost(CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            return "";
        }
        String host = text.toString().trim().toLowerCase(java.util.Locale.ROOT);
        if (host.startsWith("www.")) {
            host = host.substring(4);
        }
        return host;
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
