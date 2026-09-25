package ml.docilealligator.infinityforreddit.layout

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import java.io.File
import ml.docilealligator.infinityforreddit.R
import ml.docilealligator.infinityforreddit.customviews.SignalNavigationItemView
import ml.docilealligator.infinityforreddit.font.FontFamily
import ml.docilealligator.infinityforreddit.font.FontStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 411dp is below the sw600dp threshold, so this picks the phone shell. `night` makes the render use
// the dark palette, because dark-on-dark text in a screenshot artifact is indistinguishable from no
// text at all.
private const val PHONE_QUALIFIERS = "w411dp-h891dp-xxhdpi-night"
private const val TABLET_QUALIFIERS = "sw600dp-w1280dp-h800dp-xhdpi"
private const val REPORT_DIR = "build/reports/shell"

/**
 * Measures the MainActivity shell the way the device does, without an emulator.
 *
 * The shell has two variants and both are covered here, because they fail differently and both
 * failures look like an empty feed on a screenshot:
 *
 *  - The phone shell puts the global navigation in a `BottomAppBar` that shares the CoordinatorLayout
 *    with the app bar and the pager. Anything that lets that bar measure taller than one row makes it
 *    an opaque sheet drawn over both, so the toolbar and the feed vanish behind it and a FAB
 *    anchored to it lands in the middle of the window. That is what the first preview shipped.
 *  - The `sw600dp` and `layout-land` shells swap the bar for a navigation rail beside the pager,
 *    inside a LinearLayout. There the pager's height is what decides whether the feed appears at
 *    all, and a `wrap_content` pager measures to nothing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class, qualifiers = PHONE_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MainShellLayoutTest {

    @Test
    fun phoneShellKeepsGlobalNavigationOneRowAtTheBottom() {
        val shell = inflateShell(PHONE_QUALIFIERS, "phone")

        val navigationBar = shell.requireView(R.id.bottom_app_bar_bottom_app_bar)
        // bindOptionDrawableResource() shows the bar at runtime; it ships GONE.
        navigationBar.visibility = View.VISIBLE
        val feedItem = shell.requireView(R.id.option_1_bottom_app_bar) as SignalNavigationItemView
        // setBottomAppBarContentDescription() labels the items at runtime; the geometry below only
        // means anything once a label is there to be clipped.
        feedItem.setLabel("Feed")
        val measured = measure(shell, "phone")

        val rowHeight = shell.resources.getDimensionPixelSize(R.dimen.navigation_item_min_height)
        assertEquals(
            "navigation bar must measure exactly one row. $measured",
            rowHeight,
            navigationBar.height,
        )
        assertEquals(
            "navigation bar must sit flush with the bottom of the shell. $measured",
            shell.height,
            navigationBar.bottom,
        )
        val label = requireNotNull(feedItem.findTextView()) { "navigation item has no label view" }
        assertEquals("label must carry the destination's name", "Feed", label.text.toString())
        assertEquals(
            "label must be visible at the default font scale. $measured",
            View.VISIBLE,
            label.visibility,
        )
        val labelBounds = boundsWithin(label, feedItem)
        assertTrue(
            "navigation label must fit inside its row; label=$labelBounds row=0..${feedItem.height}. $measured",
            labelBounds.top >= 0 && labelBounds.bottom <= feedItem.height && labelBounds.height() > 0,
        )
        val pager = shell.requireView(R.id.view_pager_main_activity)
        // ScrollingViewBehavior offsets the pager by the app bar, so its height is the window minus
        // the app bar's scroll range and its bottom runs past the bar's top on purpose: the feed
        // scrolls under the bar and keeps its clearance as bottom padding.
        assertTrue(
            "feed pager must fill the window under the app bar. $measured",
            pager.height >= shell.height * 3 / 4,
        )
        assertTrue(
            "feed must reach the bottom edge and scroll under the navigation bar. $measured",
            pager.bottom >= shell.height,
        )
    }

    @Test
    fun navigationRowSurvivesALargeFontScale() {
        RuntimeEnvironment.setFontScale(2f)
        val shell = inflateShell(PHONE_QUALIFIERS, "phone-font200")
        val navigationBar = shell.requireView(R.id.bottom_app_bar_bottom_app_bar)
        navigationBar.visibility = View.VISIBLE
        val feedItem = shell.requireView(R.id.option_1_bottom_app_bar) as SignalNavigationItemView
        feedItem.setLabel("Feed")
        val measured = measure(shell, "phone-font200")

        val icon = requireNotNull(feedItem.findImageView()) { "navigation item has no icon" }
        val iconBounds = boundsWithin(icon, feedItem)
        assertTrue(
            "the icon must stay inside its row at 200% font. icon=$iconBounds row=0..${feedItem.height}. $measured",
            iconBounds.top >= 0 && iconBounds.bottom <= feedItem.height,
        )
        // Whether the label survives a doubled font is a design choice; overflowing the row is not.
        feedItem.findTextView()?.let { label ->
            val labelBounds = boundsWithin(label, feedItem)
            assertTrue(
                "a visible label must fit inside its row. label=$labelBounds row=0..${feedItem.height}. $measured",
                labelBounds.top >= 0 && labelBounds.bottom <= feedItem.height,
            )
        }
    }

    private fun View.findTextView(): TextView? {
        if (this is TextView) {
            return this
        }
        if (this !is ViewGroup) {
            return null
        }
        for (i in 0 until childCount) {
            getChildAt(i).findTextView()?.let { return it }
        }
        return null
    }

    private fun View.findImageView(): ImageView? {
        if (this is ImageView) {
            return this
        }
        if (this !is ViewGroup) {
            return null
        }
        for (i in 0 until childCount) {
            getChildAt(i).findImageView()?.let { return it }
        }
        return null
    }

    private fun boundsWithin(view: View, ancestor: View): Rect {
        val bounds = Rect(0, 0, view.width, view.height)
        var current: View = view
        while (current !== ancestor) {
            bounds.offset(current.left, current.top)
            current = current.parent as View
        }
        return bounds
    }

    @Test
    @Config(qualifiers = TABLET_QUALIFIERS)
    fun railShellKeepsTheFeedAtFullHeight() {
        val shell = inflateShell(TABLET_QUALIFIERS, "tablet")
        assertNotNull(
            "the sw600dp shell is the rail variant",
            shell.findViewById<View>(R.id.navigation_rail),
        )
        val measured = measure(shell, "tablet")

        assertTrue(
            "the rail variant has no bottom bar to look for. $measured",
            shell.findViewById<View>(R.id.bottom_app_bar_bottom_app_bar) == null,
        )
        assertTrue(
            "feed pager must fill the window beside the rail. $measured",
            shell.requireView(R.id.view_pager_main_activity).height >= shell.height * 3 / 4,
        )
    }

    private fun inflateShell(qualifiers: String, label: String): FrameLayout {
        val activity = themedActivity()
        val shell = FrameLayout(activity)
        // A real parent is required: the shell reaches the app bar, the pager and the navigation
        // bar through nested <include>s, and a null root inflates those subtrees detached.
        shell.addView(LayoutInflater.from(activity).inflate(R.layout.activity_main, shell, false))
        // Gradle's console prints only the exception type, so the view tree and the render go to
        // the artifact instead: between them they show what a variant actually produced.
        File(REPORT_DIR).mkdirs()
        File("$REPORT_DIR/$label-tree.txt").writeText(describe(shell))
        return shell
    }

    private fun themedActivity(): Activity {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        // The theme stack BaseActivity builds at runtime, so `?attr/` references in the shell
        // resolve the same way they do on the device.
        activity.theme.applyStyle(R.style.Theme_Normal_AmoledDark, true)
        activity.theme.applyStyle(FontStyle.Normal.resId, true)
        activity.theme.applyStyle(FontFamily.Default.resId, true)
        return activity
    }

    private fun measure(shell: FrameLayout, label: String): String {
        val width = shell.resources.displayMetrics.widthPixels
        val height = shell.resources.displayMetrics.heightPixels
        shell.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
        )
        shell.layout(0, 0, width, height)
        val geometry = "shell=${shell.width}x${shell.height} " + describe(shell)
        File("$REPORT_DIR/$label-measured.txt").writeText(geometry)
        writeRender(shell, "$REPORT_DIR/$label-shell.png")
        return geometry
    }

    /**
     * Roborazzi's capture needs a plugin task to be wired up; drawing the view straight into a
     * bitmap is one line and puts a real picture of the shell in the artifact, which is the only
     * way to *see* what the numbers say.
     */
    private fun writeRender(shell: FrameLayout, path: String) {
        val bitmap = Bitmap.createBitmap(shell.width, shell.height, Bitmap.Config.ARGB_8888)
        shell.draw(Canvas(bitmap))
        File(path).outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    private fun FrameLayout.requireView(id: Int): View = requireNotNull(findViewById(id)) {
        "view ${resources.getResourceEntryName(id)} is not in the shell"
    }

    private fun describe(view: View, depth: Int = 0): String = buildString {
        repeat(depth) { append("  ") }
        append(view.javaClass.simpleName)
        if (view.id != View.NO_ID) {
            // Framework ids (android.R.id.*) are not in the app's resource table.
            val name = runCatching { view.resources.getResourceEntryName(view.id) }.getOrNull()
            append(if (name != null) " #$name" else " #0x${Integer.toHexString(view.id)}")
        }
        if (view is ViewGroup) {
            append(" children=").append(view.childCount)
            append(" size=${view.width}x${view.height} at ${view.left},${view.top}")
        }
        append('\n')
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                append(describe(view.getChildAt(i), depth + 1))
            }
        }
    }
}