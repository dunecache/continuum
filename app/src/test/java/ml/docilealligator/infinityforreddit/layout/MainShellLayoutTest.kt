package ml.docilealligator.infinityforreddit.layout

import android.app.Activity
import android.app.Application
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import ml.docilealligator.infinityforreddit.R
import ml.docilealligator.infinityforreddit.font.FontFamily
import ml.docilealligator.infinityforreddit.font.FontStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 411dp is below the sw600dp threshold, so this picks the phone shell.
private const val PHONE_QUALIFIERS = "w411dp-h891dp-xxhdpi"
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
        assertEquals(
            "feed pager must fill the window under the app bar. $measured",
            shell.height,
            shell.requireView(R.id.view_pager_main_activity).height,
        )
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
        shell.captureRoboImage(filePath = "$REPORT_DIR/$label-shell.png")
        return "shell=${shell.width}x${shell.height} " + describe(shell)
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