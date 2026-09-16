package com.gantree.cab.ui

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.gantree.cab.mailbox.DEFAULT_FONT
import com.gantree.cab.mailbox.DEFAULT_LANG
import com.gantree.cab.mailbox.DEFAULT_PHOTO_SIZE
import com.gantree.cab.mailbox.DEFAULT_THEME
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The closed-set settings are dropdowns: one field each, the choices under it, a pick reports the id. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CabSettingsTest {
  @get:Rule
  val compose = createComposeRule()

  private val picked = mutableListOf<Pair<String, String>>()

  private fun settings(voiceOffered: Boolean = true) {
    compose.setContent {
      CabTheme(themeId = DEFAULT_THEME, fontId = DEFAULT_FONT) {
        CabSettings(
          origin = "https://pendant.example",
          slug = "kit",
          spike = "",
          email = "",
          googleReady = false,
          cranes = emptyList(),
          themeId = DEFAULT_THEME,
          fontId = DEFAULT_FONT,
          onOrigin = {},
          onSlug = {},
          onSpike = {},
          onTheme = { picked += "theme" to it },
          onFont = { picked += "font" to it },
          onConnect = {},
          onGoogle = {},
          onSignOut = {},
          photoSizeId = DEFAULT_PHOTO_SIZE,
          onPhotoSize = { picked += "photo" to it },
          voiceOffered = voiceOffered,
          langId = DEFAULT_LANG,
          onLang = { picked += "lang" to it },
        )
      }
    }
  }

  /** The drawer scrolls and the test screen is short: bring the field up, then tap it open. */
  private fun open(label: String) {
    compose.onNodeWithContentDescription(label).performScrollTo().performClick()
  }

  @Test
  fun eachPickerIsOneFieldShowingTheCurrentChoice() {
    settings()
    compose.onNodeWithContentDescription("Language").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("English").assertIsDisplayed()
    compose.onNodeWithContentDescription("Theme").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("Boom").assertIsDisplayed()
    compose.onNodeWithContentDescription("Font size").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("Small").assertIsDisplayed()
    compose.onNodeWithContentDescription("Photo size").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("Medium · 1024 px").assertIsDisplayed()
  }

  @Test
  fun openingThemeListsTheCatalogAndAPickReportsTheId() {
    settings()
    open("Theme")
    compose.onNodeWithContentDescription("Boom").assertIsSelected()
    compose.onNodeWithContentDescription("Lamp").performClick()
    assertEquals(listOf("theme" to "lamp"), picked)
    // The menu closed and the field shows the pick's label.
    compose.onNodeWithContentDescription("Inlay").assertDoesNotExist()
  }

  @Test
  fun fontLanguageAndPhotoPickTheirIds() {
    settings()
    open("Font size")
    compose.onNodeWithContentDescription("Extra large").performClick()
    open("Language")
    compose.onNodeWithContentDescription("日本語 · Japanese").performClick()
    open("Photo size")
    compose.onNodeWithContentDescription("Small · 640 px").performClick()
    assertEquals(listOf("font" to "xl", "lang" to "ja", "photo" to "small"), picked)
  }

  @Test
  fun languageOnlyShowsWhenTheWorkerPublishesVoice() {
    settings(voiceOffered = false)
    compose.onNodeWithContentDescription("Theme").performScrollTo().assertIsDisplayed()
    compose.onNodeWithContentDescription("Language").assertDoesNotExist()
  }
}
