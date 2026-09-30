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
 * The default feed family's post card: the one `POST_LAYOUT_CARD` inflates for every post type,
 * which is text, preview, gallery, and both autoplay video controllers.
 *
 * The family used to be the one card in the app that no phase had touched, carrying a 2dp
 * `MaterialCardView` elevation, 16dp corners, hardcoded margins, an uncapped title and a badge row
 * that reserved a full row of space on every post whether or not the post had a badge. It is also
 * the family that cannot be verified by looking at a style, because the adapter repaints the card on
 * every bind: see the read-state test below for why the surface has to be checked where it is
 * actually decided.
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

    /**
     * Every layout in the family has to be on the same card style, and none may go back to the
     * elevated Material one.
     *
     * Read from the source rather than from a painted card for the same reason the shell's chrome
     * test is: the card's own background is replaced at bind time, so a value found on an inflated
     * view is the adapter's doing and says nothing about what the layout declared. What can drift
     * silently here is a layout left behind on the old style, which is exactly what happened to the
     * five card_3 layouts while the default family was skipped.
     */
    @Test
    fun everyDefaultFamilyLayoutDeclaresTheTonalCardAndNotTheElevatedOne() {
        for (name in defaultFamilyLayouts) {
            val layout = File("src/main/res/layout/$name").readText()
            assertTrue(
                "$name declares no card style, so this would pass on anything",
                layout.contains("style=\"@style/Widget.Continuum.PostCard.Large\""),
            )
            assertFalse(
                "$name still uses the elevated Material card, which brings the 2dp shadow back",
                layout.contains("materialCardViewElevatedStyle"),
            )
            assertFalse(
                "$name still sets a card elevation, which overrides the style's 0dp",
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
