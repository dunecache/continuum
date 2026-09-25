package ml.docilealligator.infinityforreddit.customviews;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.widget.AppCompatTextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.color.MaterialColors;
import java.util.Locale;
import ml.docilealligator.infinityforreddit.R;

/**
 * The domain of a link post, as a quiet tonal chip instead of a bare line of text.
 *
 * <p>Phase 1 of docs/REDESIGN.md asks for a rounded domain chip with a favicon. The chip leads with
 * the site's own {@code /favicon.ico} once it arrives and falls back to the app's link glyph until
 * then, or for good when a site has none. Glide caches by URL, so a feed that shows the same domain
 * repeatedly costs one request.
 *
 * <p>Adapters bind these through {@code setText}, so the host is normalised here rather than at
 * every call site: {@code https://www.Example.com/path} reads as {@code example.com}, and that is
 * also the cache key for the icon.
 */
public final class LinkDomainView extends AppCompatTextView {

    private final int iconSize;
    @Nullable
    private RequestManager requestManager;
    @Nullable
    private Target<Drawable> faviconTarget;
    @Nullable
    private String pendingHost;
    @Nullable
    private String loadedHost;
    private boolean showingFavicon;

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
        iconSize = getResources().getDimensionPixelSize(R.dimen.domain_chip_icon_size);
        setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.space_8));

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(getResources().getDimension(R.dimen.shape_corner_small));
        background.setColor(MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorSurfaceContainerHigh));
        setBackground(background);

        setTextColor(MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnSurfaceVariant));
        showFallbackGlyph();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Normalises the host, hides the chip when there is nothing to show, and asks for the site's
     * icon. A recycled row cannot keep the domain, or the icon, of the post it used to hold.
     */
    @Override
    public void setText(CharSequence text, BufferType type) {
        String host = normalizeHost(text);
        super.setText(host, type);
        setVisibility(host.isEmpty() ? GONE : VISIBLE);
        if (host.isEmpty()) {
            cancelFavicon();
            return;
        }
        if (host.equals(loadedHost)) {
            return;
        }
        pendingHost = host;
        // Only load once attached: Glide needs a live context, and a row bound during a layout pass
        // (or a screenshot test) must not start a request at all.
        if (isAttachedToWindow()) {
            loadFavicon(host);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (pendingHost != null && !pendingHost.equals(loadedHost)) {
            loadFavicon(pendingHost);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelFavicon();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility != VISIBLE) {
            // The adapters hide the chip to recycle the row, which is the only signal this view gets
            // that the request it started is no longer wanted.
            cancelFavicon();
        }
    }

    @Override
    public void setTextColor(int color) {
        super.setTextColor(color);
        // A site's own icon keeps its colours; only the fallback glyph follows the chip's text.
        if (!showingFavicon) {
            applyTintToCompoundDrawables(color);
        }
    }

    private void loadFavicon(String host) {
        cancelFavicon();
        String url = faviconUrl(host);
        if (url == null) {
            loadedHost = host;
            return;
        }
        try {
            requestManager = Glide.with(this);
            faviconTarget = requestManager.load(url)
                    // The icon is drawn at 16dp; fetching a 256px one and downsampling wastes the
                    // user's data on a list that may show a dozen link posts.
                    .override(iconSize, iconSize)
                    .dontAnimate()
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource,
                                                    @Nullable Transition<? super Drawable> transition) {
                            if (host.equals(pendingHost)) {
                                loadedHost = host;
                                showingFavicon = true;
                                setCompoundDrawablesRelative(resource, null, null, null);
                                sizeCompoundDrawables();
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            if (host.equals(pendingHost)) {
                                loadedHost = host;
                                showFallbackGlyph();
                            }
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            if (host.equals(pendingHost)) {
                                // Plenty of sites have no favicon.ico and some block the request.
                                // The glyph is the answer, not an error the user needs to see.
                                loadedHost = host;
                                showFallbackGlyph();
                            }
                        }
                    });
        } catch (IllegalArgumentException e) {
            // A chip must never be able to take the feed down with it; the glyph is already showing.
            loadedHost = host;
        }
    }

    private void cancelFavicon() {
        if (requestManager != null && faviconTarget != null) {
            requestManager.clear(faviconTarget);
        }
        faviconTarget = null;
        loadedHost = null;
        showFallbackGlyph();
    }

    private void showFallbackGlyph() {
        showingFavicon = false;
        setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_link_day_night_24dp, 0, 0, 0);
        sizeCompoundDrawables();
        applyTintToCompoundDrawables(getCurrentTextColor());
    }

    private void sizeCompoundDrawables() {
        if (iconSize <= 0) {
            return;
        }
        Drawable[] drawables = getCompoundDrawables();
        for (Drawable drawable : drawables) {
            if (drawable != null) {
                drawable.setBounds(0, 0, iconSize, iconSize);
            }
        }
    }

    private void applyTintToCompoundDrawables(int color) {
        Drawable[] drawables = getCompoundDrawables();
        for (Drawable drawable : drawables) {
            if (drawable != null) {
                drawable.setTint(color);
            }
        }
    }

    @VisibleForTesting
    static String normalizeHost(@Nullable CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            return "";
        }
        String host = text.toString().trim().toLowerCase(Locale.ROOT);
        // The feed adapters pass Uri.getHost() already, but a full URL should not turn the chip into
        // a wall of text if a future call site is less careful.
        if (host.contains("://") || host.contains("/")) {
            String parsed = Uri.parse(host).getHost();
            if (parsed != null) {
                host = parsed;
            }
        }
        if (host.startsWith("www.")) {
            host = host.substring(4);
        }
        return host;
    }

    @Nullable
    private static String faviconUrl(String host) {
        if (host.isEmpty() || !host.contains(".")) {
            return null;
        }
        try {
            return new Uri.Builder().scheme("https").authority(host).path("/favicon.ico").build()
                    .toString();
        } catch (UnsupportedOperationException e) {
            return null;
        }
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
