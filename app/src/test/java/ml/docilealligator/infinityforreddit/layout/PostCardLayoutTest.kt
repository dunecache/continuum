package ml.docilealligator.infinityforreddit.layout

import android.app.Activity
import android.app.Application
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.widget.FrameLayout
import com.google.android.material.color.MaterialColors
import java.io.File
import ml.docilealligator.infinityforreddit.R
import ml.docilealligator.infinityforreddit.databinding.ItemPostWithPreviewBinding
import ml.docilealligator.infinityforreddit.font.ContentFontFamily
import ml.docilealligator.infinityforreddit.font.ContentFontStyle
import ml.docilealligator.infinityforreddit.font.FontFamily
import ml.docilealligator.infinityforreddit.font.FontStyle
import ml.docilealligator.infinityforreddit.font.TitleFontFamily
import ml.docilealligator.infinityforreddit.font.TitleFontStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val PHONE_QUALIFIERS = "w411dp-h891dp-xxhdpi"

/**
 * The default feed family's post row: the one `POST_LAYOUT_CARD` inflates for every post type,
 * which is text, preview, gallery, and both autoplay video controllers.
 *
 * The family used to be the one row in the app that no phase had touched, carrying a 2dp
 * `MaterialCardView` elevation, 16dp corners, hardcoded margins, an uncapped title and a badge row
 * that reserved a full row of space on every post whether or not the post had a badge. It has since
 * lost the card entirely: it is a plane now, with a hairline between rows rather than a gap between
 * boxes. That change is invisible in a screenshot of a single post, which is why the shape is
 * asserted from the source below - and it is still not verifiable by looking at a style, because the
 * adapter repaints the row on every bind: see the read-state test for why the surface has to be
 * checked where it is actually decided.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class, qualifiers = PHONE_QUALIFIERS)
class PostCardLayoutTest {

    private val defaultFamilyLayouts = listOf(
        "item_post_with_preview.xml",
        "item_post_text.xml",
        "item_post_gallery_type.xml",
        "item_post_video_type_autoplay.xml",
        "item_post_video_type_autoplay_legacy_controller.xml",
    )

    /** The other user-selectable feed family, which is the one that used to drift. */
    private val card3FamilyLayouts = listOf(
        "item_post_card_3_with_preview.xml",
        "item_post_card_3_text.xml",
        "item_post_card_3_gallery_type.xml",
        "item_post_card_3_video_type_autoplay.xml",
        "item_post_card_3_video_type_autoplay_legacy_controller.xml",
    )

    /**
     * Every post row in both selectable families is a plane, not a card.
     *
     * The families used to declare `Widget.Continuum.PostCard.Large`, which is gone: a post row is
     * no longer a `MaterialCardView` at all, because a radius and 8dp of margin above and below
     * every row is what made a feed read as a column of tiles, and REDESIGN.md rules that out twice
     * over ("no stock Material demo look", "tonal surfaces instead of hard borders").
     *
     * Read from the source for the same reason as the shell's chrome test: the row's background is
     * replaced at bind time by `setBackgroundTintList`, so a value on an inflated view is the
     * adapter's doing and says nothing about what the layout declared. What can drift here is a
     * layout left behind - and it did: the card_3 family sat on the old style while the default
     * family moved, which is exactly what a per-family test catches and a one-file test never will.
     *
     * The card_3 family is in the list rather than trusted, because that is the family whose root
     * was a `TouchInterceptableMaterialCardView` - a subclass whose only method,
     * `setShouldInterceptTouch`, nothing in the app ever called, so it was already a plain card.
     */
    @Test
    fun everyPostRowDeclaresThePlaneAndIsNotACard() {
        for (name in defaultFamilyLayouts + card3FamilyLayouts) {
            val layout = File("src/main/res/layout/$name").readText()
            // Comments are stripped before the root is read, because the layouts explain this
            // decision in a comment directly above it - and that comment names the card class it
            // replaced, which a naive substring would read as the root still being one.
            val body = layout.replace(Regex("""<!--[\s\S]*?-->"""), "")
                .substringAfterLast("?>").trim()
            val root = body.substringBefore('>')

            assertFalse(
                "$name is still rooted in a card ($root). A post row is a plane; the only things " +
                    "left as cards are the gallery's media tiles and the post-detail search panel.",
                root.contains("CardView"),
            )
            // Scoped to the root: margins inside a row are the card's own spacing and stay. It is
            // the margin *around* a row - the gap that used to separate one card from the next -
            // that the hairline replaces, and only the root could ever have declared it.
            assertFalse(
                "$name still gives its rows a margin, which is the gap the hairline replaces: " +
                    root.substringAfter("android:layout_width"),
                Regex("""android:layout_margin(Start|End|Top|Bottom|Begin|End)=""").containsMatchIn(root),
            )
            assertTrue(
                "$name declares no surface, so this would pass on anything",
                layout.contains("""android:background="@drawable/continuum_post_surface""""),
            )
            assertTrue(
                "$name must draw the hairline that replaces the gap between cards, or two rows " +
                    "sharing an edge read as one",
                layout.contains("""android:background="?attr/colorOutlineVariant""""),
            )
            assertFalse(
                "$name still declares a card style, whose radius and fill the design has dropped",
                layout.contains("Widget.Continuum.PostCard"),
            )
            assertFalse(
                "$name still uses the elevated Material card, which brings the 2dp shadow back",
                layout.contains("materialCardViewElevatedStyle"),
            )
            assertFalse(
                "$name still sets a card elevation",
                layout.contains("cardElevation"),
            )
        }
    }

    /**
     * An unread post and a read post have to land on different surfaces in every theme.
     *
     * This is the guard for the one thing about this card that a style cannot express.
     * `setItemViewBackgroundColor()` calls `setBackgroundTintList()` on the card on every bind and
     * on every swipe, and on a `MaterialCardView` that replaces `cardBackgroundColor` outright, so
     * whatever `Widget.Continuum.PostCard` declares is gone by the time the row is on screen. The
     * two colours are therefore chosen in the adapter, and if the two roles ever resolve to the same
     * value the read state stops being visible at all with nothing to fail a build.
     *
     * The three themes are named the way `BaseActivity` names them (BaseActivity.java:168-199),
     * because those are the styles that carry the roles: applying a bare `AppTheme` would resolve
     * Material's stock M3 values instead of the app's and the test would pass for the wrong reason.
     */
    @Test
    fun theUnreadAndReadCardSurfacesDifferInEveryTheme() {
        val themes = listOf(
            R.style.Theme_Normal,
            R.style.Theme_Normal_NormalDark,
            R.style.Theme_Normal_AmoledDark,
        )
        for (theme in themes) {
            val activity = themedActivity(theme)
            val unread = MaterialColors.getColor(
                activity, com.google.android.material.R.attr.colorSurfaceContainerHigh, 0,
            )
            val read = MaterialColors.getColor(
                activity, com.google.android.material.R.attr.colorSurfaceContainerLow, 0,
            )
            assertNotEquals(
                "neither role resolved under theme $theme, so the card has no surface at all",
                0, unread,
            )
            assertNotEquals(
                "neither role resolved under theme $theme, so the card has no surface at all",
                0, read,
            )
            assertNotEquals(
                "an unread post and a read post resolve to the same surface under theme $theme, " +
                    "so marking one read would be invisible",
                unread, read,
            )
        }
    }

    /**
     * The vote arrows and the row's other controls have to stay tappable at the size the redesign
     * asks for, which is measured here rather than read from the layout: `minWidth` on a
     * `MaterialButton` competes with the widget's own insets, so the declared value is not the
     * value the finger gets.
     */
    @Test
    fun everyCardActionControlMeasuresAtLeastTheTouchTargetFloor() {
        val card = inflateCard().root
        val floor = card.resources.getDimensionPixelSize(R.dimen.touch_target_min)
        val controls = mapOf(
            R.id.upvote_button_item_post_with_preview to "upvote",
            R.id.downvote_button_item_post_with_preview to "downvote",
            R.id.comments_count_button_item_post_with_preview to "comments",
            R.id.save_button_item_post_with_preview to "save",
            R.id.share_button_item_post_with_preview to "share",
        )
        for ((id, name) in controls) {
            val control = requireNotNull(card.findViewById<View>(id)) { "no $name control on the card" }
            assertTrue(
                "the $name control is ${control.measuredWidth}x${control.measuredHeight}, " +
                    "under the ${floor}x$floor floor",
                control.measuredWidth >= floor && control.measuredHeight >= floor,
            )
        }
    }

    /**
     * The title is the card's only display type and the one element that can grow without bound, so
     * it is capped and ellipsized. A long title that wraps instead pushes the media and the action
     * row down a tall card, and the post detail is where the rest of the title is read.
     */
    @Test
    fun theCardTitleIsCappedAtThreeLinesAndEllipsized() {
        val binding = inflateCard()

        assertEquals(
            "the title must not wrap past three lines",
            3, binding.titleTextViewItemPostWithPreview.maxLines,
        )
        assertEquals(
            "a capped title has to say what it cut off",
            android.text.TextUtils.TruncateAt.END,
            binding.titleTextViewItemPostWithPreview.ellipsize,
        )
    }

    private fun inflateCard(): ItemPostWithPreviewBinding {
        val activity = themedActivity(R.style.Theme_Normal)
        val parent = FrameLayout(activity)
        val binding = ItemPostWithPreviewBinding.inflate(
            LayoutInflater.from(activity), parent, false,
        )
        val width = activity.resources.displayMetrics.widthPixels
        parent.addView(binding.root)
        parent.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        )
        parent.layout(0, 0, parent.measuredWidth, parent.measuredHeight)
        return binding
    }

    private fun themedActivity(theme: Int): Activity {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        // The theme stack BaseActivity builds at runtime, so the font attributes a TextAppearance
        // needs are set here the way the device sets them. A card that inflates without them fails
        // rather than merely looking wrong.
        activity.theme.applyStyle(theme, true)
        activity.theme.applyStyle(FontStyle.Normal.resId, true)
        activity.theme.applyStyle(TitleFontStyle.Normal.resId, true)
        activity.theme.applyStyle(ContentFontStyle.Normal.resId, true)
        activity.theme.applyStyle(FontFamily.Default.resId, true)
        activity.theme.applyStyle(TitleFontFamily.Default.resId, true)
        activity.theme.applyStyle(ContentFontFamily.Default.resId, true)
        return activity
    }
}
