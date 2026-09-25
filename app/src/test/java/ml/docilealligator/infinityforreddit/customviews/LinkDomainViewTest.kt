package ml.docilealligator.infinityforreddit.customviews

import android.app.Activity
import android.app.Application
import android.view.View
import ml.docilealligator.infinityforreddit.R
import ml.docilealligator.infinityforreddit.font.FontFamily
import ml.docilealligator.infinityforreddit.font.FontStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The domain chip hides and shows itself from `setText`, because the adapters bind it through that
 * and a recycled row must not keep the domain of the post it used to hold.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class LinkDomainViewTest {

    @Test
    fun hostIsLowercasedAndWwwStripped() {
        assertEquals("example.com", LinkDomainView.normalizeHost("https://WWW.Example.com/some/path"))
        assertEquals("example.com", LinkDomainView.normalizeHost("  Example.COM  "))
        assertEquals("news.example.co.uk", LinkDomainView.normalizeHost("news.example.co.uk"))
    }

    @Test
    fun missingHostNormalisesToNothing() {
        assertEquals("", LinkDomainView.normalizeHost(null))
        assertEquals("", LinkDomainView.normalizeHost(""))
    }

    @Test
    fun chipHidesItselfWhenThereIsNoDomain() {
        val chip = chip()
        chip.setText("example.com")
        assertEquals(View.VISIBLE, chip.visibility)

        chip.setText("")
        assertEquals(View.GONE, chip.visibility)

        chip.setText("example.com")
        assertEquals(View.VISIBLE, chip.visibility)
    }

    @Test
    fun chipKeepsTheFallbackGlyphWhileNoFaviconHasArrived() {
        val chip = chip()
        // A detached view never starts a request, which is also what keeps screenshot tests and
        // layout passes off the network.
        chip.setText("example.com")
        assertNotNull(chip.getCompoundDrawablesRelative()[0])
        assertEquals("example.com", chip.text.toString())
    }

    private fun chip(): LinkDomainView {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        activity.theme.applyStyle(R.style.Theme_Normal_NormalDark, true)
        activity.theme.applyStyle(FontStyle.Normal.resId, true)
        activity.theme.applyStyle(FontFamily.Default.resId, true)
        return LinkDomainView(activity)
    }
}
