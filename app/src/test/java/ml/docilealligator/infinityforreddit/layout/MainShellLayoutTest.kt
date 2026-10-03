package ml.docilealligator.infinityforreddit.layout

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.color.MaterialColors
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.tabs.TabLayout
import java.io.File
import kotlin.math.roundToInt
import ml.docilealligator.infinityforreddit.R
import ml.docilealligator.infinityforreddit.customviews.NavigationWrapper
import ml.docilealligator.infinityforreddit.customviews.SignalNavigationItemView
import ml.docilealligator.infinityforreddit.font.ContentFontFamily
import ml.docilealligator.infinityforreddit.font.ContentFontStyle
import ml.docilealligator.infinityforreddit.font.FontFamily
import ml.docilealligator.infinityforreddit.font.FontStyle
import ml.docilealligator.infinityforreddit.font.TitleFontFamily
import ml.docilealligator.infinityforreddit.font.TitleFontStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
private const val PHONE_QUALIFIERS = "w411dp-h891dp-night-xxhdpi"
private const val TABLET_QUALIFIERS = "sw600dp-w1280dp-h800dp-xhdpi"
private const val REPORT_DIR = "build/reports/shell"

/**
 * The inset attributes that take width off the selected tab's pill, and the pair that does not.
 *
 * An `android:insetLeft`/`android:insetRight` pair is a subtraction, not a padding: Material hands
 * the indicator drawable a box it has already narrowed to the label, so anything taken off the sides
 * comes out of the text inside the pill.
 */
private val HORIZONTAL_INSETS = setOf("Left", "Right")
private val VERTICAL_INSETS = setOf("Top", "Bottom")

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
        // Robolectric will not rasterise the text, so "it has ink" has to be argued rather than
        // measured: a label in the bar's own colour is invisible no matter that it is laid out well.
        val barColour = MaterialColors.getColor(
            navigationBar,
            com.google.android.material.R.attr.colorSurface,
        )
        val labelColour = label.currentTextColor
        assertTrue("navigation label must not be transparent", Color.alpha(labelColour) > 0)
        assertTrue(
            "navigation label (#%06X) must contrast with the bar (#%06X)".format(
                labelColour and 0xFFFFFF,
                barColour and 0xFFFFFF,
            ),
            colourDistance(labelColour, barColour) > 24,
        )
        assertIndicatorIsWhole(feedItem, measured)
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
    fun primaryNavigationKeepsItsRowAtALargeFontScale() {
        RuntimeEnvironment.setFontScale(2f)
        val shell = inflateShell(PHONE_QUALIFIERS, "phone-primary-nav-font200")
        val navigation = shell.requireView(R.id.bottom_navigation_main_activity) as BottomNavigationView
        navigation.visibility = View.VISIBLE
        val measured = measure(shell, "phone-primary-nav-font200")

        val row = navigation.requireMenuRow()
        val rowBounds = boundsWithin(row, navigation)
        assertTrue(
            "a doubled font must not push the destinations out of the bar; row=$rowBounds " +
                "bar=0..${navigation.height}. $measured",
            rowBounds.height() > 0 && rowBounds.bottom <= navigation.height,
        )
        // The labels are the thing that gives way at a large font, exactly as they do on the legacy
        // bar: what may not give way is the row, because a bar that has grown over the feed is a
        // layout bug rather than a design choice.
        assertTrue(
            "the bar must stay one row tall at 200% font, not grow over the feed; bar=0.." +
                "${navigation.height}. $measured",
            navigation.height <= shell.resources.getDimensionPixelSize(R.dimen.navigation_item_min_height) * 2,
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
        assertIndicatorIsWhole(feedItem, measured)
        // Whether the label survives a doubled font is a design choice; overflowing the row is not.
        feedItem.findTextView()?.let { label ->
            val labelBounds = boundsWithin(label, feedItem)
            assertTrue(
                "a visible label must fit inside its row. label=$labelBounds row=0..${feedItem.height}. $measured",
                labelBounds.top >= 0 && labelBounds.bottom <= feedItem.height,
            )
        }
    }

    /**
     * The primary navigation: five destinations, in reading order, labels always shown, and a
     * surface that reaches the bottom edge of the screen with the row held above the system inset.
     */
    @Test
    fun primaryNavigationHasFiveLabelledDestinations() {
        val shell = inflateShell(PHONE_QUALIFIERS, "phone-primary-nav")
        val navigation = shell.requireView(R.id.bottom_navigation_main_activity) as BottomNavigationView
        // bindPrimaryNavigation() shows the bar at runtime; it ships GONE.
        navigation.visibility = View.VISIBLE
        val measured = measure(shell, "phone-primary-nav")

        val menu = navigation.menu
        assertEquals("primary navigation must have exactly five destinations. $measured", 5, menu.size())
        val expected = listOf(
            R.id.navigation_bottom_home to R.string.navigation_home,
            R.id.navigation_bottom_inbox to R.string.navigation_inbox,
            R.id.navigation_bottom_account to R.string.navigation_account,
            R.id.navigation_bottom_search to R.string.navigation_search,
            R.id.navigation_bottom_settings to R.string.navigation_settings,
        )
        for ((index, destination) in expected.withIndex()) {
            val item = menu.getItem(index)
            assertEquals(
                "destination $index must be ${shell.resources.getResourceEntryName(destination.first)}. $measured",
                destination.first,
                item.itemId,
            )
            assertEquals(
                "destination ${item.itemId} must carry a label from strings.xml. $measured",
                shell.resources.getString(destination.second),
                item.title.toString(),
            )
            assertTrue(
                "destination ${item.itemId} must have an icon. $measured",
                item.icon != null,
            )
        }
        assertEquals(
            "labels must always be shown, not only on the selected destination. $measured",
            NavigationBarView.LABEL_VISIBILITY_LABELED,
            navigation.labelVisibilityMode,
        )
        assertEquals(
            "primary navigation must sit flush with the bottom of the shell. $measured",
            shell.height,
            navigation.bottom,
        )
    }

    @Test
    fun primaryNavigationKeepsItsRowAboveTheSystemInset() {
        val shell = inflateShell(PHONE_QUALIFIERS, "phone-primary-nav-inset")
        val navigation = shell.requireView(R.id.bottom_navigation_main_activity) as BottomNavigationView
        navigation.visibility = View.VISIBLE
        val plain = measure(shell, "phone-primary-nav-inset-plain")
        val rowHeight = navigation.height
        assertTrue(
            "a navigation row must clear the 48dp touch target floor; row=$rowHeight. $plain",
            rowHeight >= shell.resources.getDimensionPixelSize(R.dimen.touch_target_min),
        )

        val inset = (48 * shell.resources.displayMetrics.density).roundToInt()
        applyInset(navigation, inset)
        val measured = measure(shell, "phone-primary-nav-inset")

        assertEquals(
            "the surface must cover the row plus the system inset. $measured",
            rowHeight + inset,
            navigation.height,
        )
        assertEquals(
            "primary navigation must reach the bottom edge of the shell. $measured",
            shell.height,
            navigation.bottom,
        )
        val row = navigation.requireMenuRow()
        val rowBounds = boundsWithin(row, navigation)
        assertTrue(
            "the destinations row must stay above the system inset; row=$rowBounds " +
                "inset=$inset. $measured",
            rowBounds.bottom <= navigation.height - inset,
        )
    }

    /** BottomNavigationView's item row is its only child; the class itself is library-internal. */
    private fun View.requireMenuRow(): View = requireNotNull(
        (this as? ViewGroup)?.let { group -> (0 until group.childCount).firstOrNull { group.getChildAt(it).height > 0 }?.let { group.getChildAt(it) } }
    ) { "navigation bar has no item row" }

    private fun applyInset(navigation: BottomNavigationView, inset: Int) {
        val layoutParams = navigation.layoutParams
        if (layoutParams is ViewGroup.MarginLayoutParams) {
            layoutParams.bottomMargin = 0
        }
        navigation.setPadding(
            navigation.paddingLeft,
            navigation.paddingTop,
            navigation.paddingRight,
            inset,
        )
        navigation.layoutParams = layoutParams
    }

    /**
     * The bar's inset handling lives on [NavigationWrapper] because four activities share it, so the
     * shell test goes through that too. It is static geometry on the bar rather than wrapper state,
     * which is also why this test does not have to stand up a rail and a theme to call it.
     */
    @Test
    fun navigationBarSurfaceReachesTheBottomEdgeUnderASystemInset() {
        val shell = inflateShell(PHONE_QUALIFIERS, "phone-inset")
        val navigationBar = shell.requireView(R.id.bottom_app_bar_bottom_app_bar)
        navigationBar.visibility = View.VISIBLE
        val feedItem = shell.requireView(R.id.option_1_bottom_app_bar) as SignalNavigationItemView
        feedItem.setLabel("Feed")
        val row = shell.requireView(R.id.linear_layout_bottom_app_bar)
        val rowHeight = shell.resources.getDimensionPixelSize(R.dimen.navigation_item_min_height)
        // A 3-button navigation bar is the tall case; a gesture handle is shorter but behaves the
        // same way, only with less to hide.
        val inset = (48 * shell.resources.displayMetrics.density).roundToInt()
        NavigationWrapper.applyBottomInset(navigationBar as BottomAppBar, inset)
        val measured = measure(shell, "phone-inset")

        // One surface from the hairline to the bottom edge of the screen. A bottom margin instead
        // left a strip of window background under the bar, which read as a bar hovering over the
        // feed rather than attached to it.
        assertEquals(
            "navigation bar must reach the bottom edge of the shell. $measured",
            shell.height,
            navigationBar.bottom,
        )
        assertEquals(
            "the bar's surface must cover its row plus the system inset. $measured",
            rowHeight + inset,
            navigationBar.height,
        )
        // The row keeps its own height and sits above the inset: padding the row instead of the bar
        // ate the row's height, which pushed the icons up and dropped the labels.
        assertEquals(
            "the destinations row must keep one row of height. $measured",
            rowHeight,
            row.height,
        )
        assertTrue(
            "the destinations row must stay above the system inset. row=0..${row.height} " +
                "inset=$inset. $measured",
            row.bottom <= navigationBar.height - inset,
        )
        // The row's top edge does not move, so the feed's bottom clearance still lines up with it.
        assertEquals(
            "the row's top edge must not move when the inset is absorbed. $measured",
            shell.height - rowHeight - inset,
            navigationBar.top,
        )
        assertIndicatorIsWhole(feedItem, measured)
        val label = requireNotNull(feedItem.findTextView()) { "navigation item has no label view" }
        assertEquals(
            "a navigation inset must not cost the row its label. $measured",
            View.VISIBLE,
            label.visibility,
        )
        val labelBounds = boundsWithin(label, feedItem)
        assertTrue(
            "navigation label must fit inside its row; label=$labelBounds row=0..${feedItem.height}. $measured",
            labelBounds.top >= 0 && labelBounds.bottom <= feedItem.height && labelBounds.height() > 0,
        )
    }

    /** Rough perceptual distance; enough to catch "the label is the bar's own colour". */
    private fun colourDistance(a: Int, b: Int): Int {
        val dr = Color.red(a) - Color.red(b)
        val dg = Color.green(a) - Color.green(b)
        val db = Color.blue(a) - Color.blue(b)
        return (dr * dr + dg * dg + db * db) / 3
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

    /**
     * The active indicator is the one part of the bar that must never be cropped: a pill cut off by
     * its row reads as a rendering fault, not as a navigation bar. Its position is a translation
     * applied at layout time, so the offset has to be added before the bounds mean anything.
     */
    private fun assertIndicatorIsWhole(item: SignalNavigationItemView, measured: String) {
        item.setActive(true)
        val indicator = item.indicatorView
        val bounds = boundsWithin(indicator, item)
        bounds.offset(0, indicator.translationY.toInt())
        assertTrue(
            "the active indicator must sit whole inside its row; indicator=$bounds " +
                "row=0..${item.height}. $measured",
            bounds.top >= 0 && bounds.bottom <= item.height && bounds.height() > 0,
        )
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

    /**
     * The shell's top bar: a title block that fits its row, and a feed strip that is pinned rather
     * than scrolling away with the title.
     *
     * Both of these are invisible in a screenshot of the resting state and both were wrong before,
     * so they are asserted as facts about the layout rather than left to the eye.
     */
    @Test
    fun shellTitleBlockFitsAndTheFeedStripIsPinned() {
        val shell = inflateShell(PHONE_QUALIFIERS, "phone-title")
        val toolbar = shell.requireView(R.id.toolbar)
        val strip = shell.requireView(R.id.tab_layout_main_activity)
        val header = requireNotNull(toolbar.findViewById<View>(R.id.feed_header_app_bar_main_activity)) {
            "the toolbar has no title block"
        }
        val measured = measure(shell, "phone-title")

        val headerBounds = boundsWithin(header, shell)
        assertTrue(
            "the title block must sit inside the toolbar; header=$headerBounds " +
                "toolbar=0..${toolbar.height}. $measured",
            headerBounds.height() > 0 && headerBounds.bottom <= toolbar.height,
        )
        assertTrue(
            "the title block must not be wider than the row it is in; header=$headerBounds " +
                "toolbar=0..${toolbar.width}. $measured",
            headerBounds.width() <= toolbar.width,
        )

        // No scroll flags is what pins it: a strip that scrolls is the strip that loses the feed you
        // are reading, which is the one thing the bar must never do.
        val stripParams = strip.layoutParams as AppBarLayout.LayoutParams
        assertEquals(
            "the feed strip must not scroll away with the title. $measured",
            0,
            stripParams.scrollFlags,
        )
        assertEquals(
            "the feed strip must be scrollable, not fill: a long feed list truncates every name " +
                "past the fourth. $measured",
            TabLayout.MODE_SCROLLABLE,
            (strip as TabLayout).tabMode,
        )
        assertTrue(
            "the strip must sit below the title it belongs to. $measured",
            strip.top >= toolbar.bottom,
        )
    }

    /**
     * The selected tab's pill covers its label, with room either side of it.
     *
     * The pill used to be inset 12dp horizontally inside a box Material had already narrowed to the
     * label - `tabIndicatorFullWidth="false"` sizes the indicator's bounds to `TabView.getContentWidth()`,
     * which is the bare TextView - so it came out narrower than the text it was behind. "Home" was a
     * 14dp sliver; "All" is a 17dp label, which Material floors at 24dp, and 12dp either side consumed
     * that whole box, so the shortest tab in the strip had no pill at all.
     *
     * Nothing about that shows in a screenshot of the resting state, and the indicator drawable's
     * bounds cannot show it either: TabLayout keeps the indicator private, and `Drawable.getBounds()`
     * on an `InsetDrawable` reports the outer box rather than the inset applied inside it. So the
     * three things the pill's width is made of are each asserted where it is actually written:
     *
     *  - `tabIndicatorFullWidth` and `tabIndicatorGravity`, read off the inflated strip. Full width
     *    means Material's bounds are the whole tab; stretch means their height is the whole row.
     *  - the tab's measured width against its label's, which is the padding the pill has to be
     *    bigger than the text by.
     *  - the indicator drawable as source, because a horizontal inset there is a second subtraction
     *    that none of the above can see.
     */
    @Test
    fun theSelectedTabPillCoversItsLabelWithRoomEitherSide() {
        val shell = inflateShell(PHONE_QUALIFIERS, "phone-tab-pill")
        val strip = shell.requireView(R.id.tab_layout_main_activity) as TabLayout
        // The default tab list's three destinations, shortest label last: a pill sized off a long label
        // looks right until the shortest label in the strip has to fit in one too.
        for (name in listOf("Home", "Popular", "All")) {
            strip.addTab(strip.newTab().setText(name))
        }
        val measured = measure(shell, "phone-tab-pill")

        // These two are what make the pill the whole tab. Read off the inflated strip rather than the
        // layout source, so a style that overrides them is caught here too.
        assertTrue(
            "the pill must span the whole tab; sized to the label instead, an inset in the indicator " +
                "drawable can only narrow it further. $measured",
            strip.isTabIndicatorFullWidth,
        )
        assertEquals(
            "the pill must stretch the strip's row, which is what leaves the bottom bar's 32dp once " +
                "the drawable's vertical inset comes off it. $measured",
            TabLayout.INDICATOR_GRAVITY_STRETCH,
            strip.tabIndicatorGravity,
        )

        val padding = shell.resources.getDimensionPixelSize(R.dimen.space_16)
        val tabsRow = requireNotNull(strip.getChildAt(0) as? ViewGroup) {
            "the strip holds no row of tabs"
        }

        for (position in 0 until strip.tabCount) {
            val name = requireNotNull(strip.getTabAt(position)) { "the strip lost tab $position" }
                .text.toString()
            val tabView = requireNotNull(tabsRow.getChildAt(position) as? ViewGroup) {
                "tab $position ($name) has no view in the row"
            }
            val label = requireNotNull(tabView.getChildAt(0) as? TextView) {
                "tab $position ($name) has no label"
            }

            assertTrue(
                "the label must measure to something, or the comparison below passes on any pill. " +
                    "tab=$position ($name) label=${label.width}px. $measured",
                label.width > 0,
            )
            assertTrue(
                "the pill spans the tab, so the tab must be wider than the label behind it by the " +
                    "strip's padding on each side; tab=$position ($name) tab=${tabView.width}px " +
                    "label=${label.width}px padding=${padding}px. $measured",
                tabView.width >= label.width + padding * 2,
            )
        }

        val indicator = File("src/main/res/drawable/tab_indicator_continuum.xml").readText()
        val insets = Regex("android:inset(\\w*)=\"(.+)\"").findAll(indicator)
            .associate { it.groupValues[1] to it.groupValues[2] }
        assertTrue(
            "the pill must not be inset horizontally: Material has already narrowed the box this " +
                "drawable is given down to the label, so an inset here takes width away from the text " +
                "inside the pill rather than padding it. Found " +
                insets.filterKeys { it in HORIZONTAL_INSETS } + ".",
            insets.keys.none { it in HORIZONTAL_INSETS },
        )
        // The vertical pair is what turns the stretched row into the bottom bar's 32dp active
        // indicator, and it is safe precisely because it shortens the pill rather than narrowing it.
        for (edge in VERTICAL_INSETS) {
            assertEquals(
                "the pill must keep the 8dp vertical inset that makes it the bottom bar's 32dp " +
                    "pill rather than a block filling the row",
                "@dimen/space_8",
                insets[edge],
            )
        }
    }

    /**
     * The shell's chrome declares the bottom navigation's surface role, and never the theme accent.
     *
     * A shared helper used to repaint the feed strip in the theme's accent, leaving a tonal toolbar
     * sitting on an accent strip, and nothing caught it.
     *
     * This reads the declared value in the layout source rather than the painted result, because
     * every widget in the app bar paints its own MaterialShapeDrawable and ignores a background set
     * in the layout - the AppBarLayout, the MaterialToolbar and the TabLayout all did, which is three
     * separate confirmations of the same thing. The surface that reaches the screen is applied at
     * runtime by MainActivity's theme method, and that is a different kind of change to test.
     */
    @Test
    fun shellChromeDeclaresTheTonalSurfaceAndNotTheAccent() {
        val layout = File("src/main/res/layout/app_bar_main.xml").readText()
        val backgrounds = Regex("""android:background="([^"]+)"""")
            .findAll(layout).map { it.groupValues[1] }.toList()

        assertTrue(
            "the shell layout declares no backgrounds, so this would pass on anything",
            backgrounds.isNotEmpty(),
        )
        for (background in backgrounds) {
            assertFalse(
                "the shell's chrome must not declare the theme accent as a background, found " +
                    background,
                background.contains("colorPrimary"),
            )
            assertEquals(
                "every surface in the shell's chrome must be the role the bottom navigation uses",
                "?attr/colorSurfaceContainerHigh",
                background,
            )
        }
    }

    /**
     * A real feed name and sort caption have to fit the block, truncating rather than wrapping or
     * pushing the bar's actions off the row.
     *
     * The block is `wrap_content`, which is what lets a long name take the space it needs and a
     * short one stay a small target, so the risk it carries is a long name growing the bar rather
     * than being cut. The title is written from a resolved tab label, and those are unbounded: a
     * user can rename a multireddit to anything.
     */
    @Test
    fun shellTitleBlockTruncatesALongFeedNameInsteadOfGrowingTheBar() {
        val shell = inflateShell(PHONE_QUALIFIERS, "phone-title-long")
        val toolbar = shell.requireView(R.id.toolbar)
        val header = requireNotNull(toolbar.findViewById<View>(R.id.feed_header_app_bar_main_activity))
        val title = requireNotNull(
            header.findViewById<TextView>(R.id.feed_title_app_bar_main_activity)
        ) { "the title block has no title" }
        val sort = requireNotNull(
            header.findViewById<TextView>(R.id.feed_sort_app_bar_main_activity)
        ) { "the title block has no sort caption" }

        // A short name first, so the comparison is against a realistic bar rather than against an
        // empty block's minimum height.
        title.text = "r/pics"
        sort.text = "Hot: Today"
        val shortHeight = measure(shell, "phone-title-long-short").let { toolbar.height }

        title.text = "r/AskRedditLikeAPersonWhoHasVeryLongSubredditNameHere"
        sort.text = "Top: This Month"
        val measured = measure(shell, "phone-title-long")

        assertEquals(
            "the title must stay one line whatever the feed is called. $measured",
            1,
            title.lineCount,
        )
        assertTrue(
            "a long feed name must not grow the bar: the actions share this row. " +
                "toolbar=0..$shortHeight now=0..${toolbar.height}. $measured",
            toolbar.height <= shortHeight,
        )
        val headerBounds = boundsWithin(header, shell)
        assertTrue(
            "the title block must still fit the row it is in; header=$headerBounds " +
                "toolbar=0..${toolbar.width}. $measured",
            headerBounds.width() <= toolbar.width,
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
        // The theme stack BaseActivity builds at runtime (BaseActivity.java:212-228), so `?attr/`
        // references in the shell resolve the same way they do on the device. All five font
        // overlays matter: a title that goes through TextAppearance.Continuum.Title needs the title
        // font attributes, and a three-overlay stack leaves them unset, which makes the inflate fail
        // rather than merely look wrong.
        activity.theme.applyStyle(R.style.Theme_Normal_AmoledDark, true)
        activity.theme.applyStyle(FontStyle.Normal.resId, true)
        activity.theme.applyStyle(TitleFontStyle.Normal.resId, true)
        activity.theme.applyStyle(ContentFontStyle.Normal.resId, true)
        activity.theme.applyStyle(FontFamily.Default.resId, true)
        activity.theme.applyStyle(TitleFontFamily.Default.resId, true)
        activity.theme.applyStyle(ContentFontFamily.Default.resId, true)
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