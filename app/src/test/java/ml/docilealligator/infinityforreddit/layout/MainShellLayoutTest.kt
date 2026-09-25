package ml.docilealligator.infinityforreddit.layout

import android.app.Activity
import android.app.Application
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import ml.docilealligator.infinityforreddit.R
import ml.docilealligator.infinityforreddit.font.FontFamily
import ml.docilealligator.infinityforreddit.font.FontStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Measures the MainActivity shell the way the device does, without an emulator.
 *
 * The global navigation bar ships after the app bar and the pager in the same CoordinatorLayout, so
 * anything that lets it measure taller than one row makes it an opaque full-screen sheet drawn on
 * top of both: the toolbar and the feed disappear behind it, and a FAB anchored to it lands in the
 * middle of the window. That is what the first preview shipped, and a screenshot is a poor guard
 * because the failure looks like an empty feed. These assertions pin the geometry instead.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class, qualifiers = "w1080dp-h2160dp-xhdpi")
class MainShellLayoutTest {

    @Test
    fun globalNavigationIsOneRowFlushWithTheBottomAndTheFeedKeepsTheWindow() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        // The theme stack BaseActivity builds at runtime, so `?attr/` references in the shell
        // resolve the same way they do on the device.
        activity.theme.applyStyle(R.style.Theme_Normal_AmoledDark, true)
        activity.theme.applyStyle(FontStyle.Normal.resId, true)
        activity.theme.applyStyle(FontFamily.Default.resId, true)

        val root = LayoutInflater.from(activity).inflate(R.layout.activity_main, null)
        // bindOptionDrawableResource() shows the bar at runtime; it ships GONE.
        val navigationBar = root.findViewById<View>(R.id.bottom_app_bar_bottom_app_bar)
        navigationBar.visibility = View.VISIBLE

        root.measure(
            MeasureSpec.makeMeasureSpec(SHELL_WIDTH_PX, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(SHELL_HEIGHT_PX, MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, SHELL_WIDTH_PX, SHELL_HEIGHT_PX)

        val shell = root.findViewById<View>(R.id.coordinator_layout_main_activity)
        val appBar = root.findViewById<View>(R.id.appbar_layout_main_activity)
        val pager = root.findViewById<View>(R.id.view_pager_main_activity)
        val rowHeight = activity.resources.getDimensionPixelSize(R.dimen.navigation_item_min_height)

        assertEquals(
            "navigation bar must measure exactly one row; a taller bar covers the feed",
            rowHeight,
            navigationBar.height,
        )
        assertEquals(
            "navigation bar must sit flush with the bottom of the shell",
            shell.height,
            navigationBar.bottom,
        )
        assertTrue(
            "navigation bar must not reach up over the app bar",
            navigationBar.top >= appBar.bottom,
        )
        assertTrue(
            "feed pager must fill the window under the app bar; was ${pager.height}px of ${shell.height}px",
            pager.height >= shell.height * 3 / 4,
        )
    }

    private companion object {
        const val SHELL_WIDTH_PX = 2160
        const val SHELL_HEIGHT_PX = 4320
    }
}
