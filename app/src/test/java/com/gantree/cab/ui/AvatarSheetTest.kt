package com.gantree.cab.ui

import android.app.Application
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.gantree.cab.drive.solidJpeg
import com.gantree.cab.mailbox.DEFAULT_FONT
import com.gantree.cab.mailbox.DEFAULT_THEME
import com.gantree.cab.mailbox.resetFileProvider
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AvatarSheetTest {
  @get:Rule
  val compose = createComposeRule()

  private val app: Application = ApplicationProvider.getApplicationContext()

  @Before
  fun freshProvider() = resetFileProvider()

  @Test
  fun copyPutsTheFaceOnTheClipboardAndReplaceOpensThePicker() {
    val face = solidJpeg(64, 64)
    var replaced = 0
    var dismissed = 0
    compose.setContent {
      CabTheme(themeId = DEFAULT_THEME, fontId = DEFAULT_FONT) {
        AvatarSheet(slug = "kit", bytes = face, onReplace = { replaced++ }, onDismiss = { dismissed++ })
      }
    }
    compose.onNodeWithText("Kit's photo").assertIsDisplayed()
    compose.onNodeWithText("Copy").assertIsDisplayed().performClick()
    compose.onNodeWithText("Copied").assertIsDisplayed()
    val clip = app.getSystemService(ClipboardManager::class.java).primaryClip!!
    val uri = clip.getItemAt(0).uri!!
    assertEquals("${app.packageName}.fileprovider", uri.authority)
    assertArrayEquals(face, app.contentResolver.openInputStream(uri)!!.use { it.readBytes() })
    assertEquals(0, dismissed)

    compose.onNodeWithText("Replace").performClick()
    assertEquals(1, replaced)
    assertEquals(1, dismissed)
  }

  @Test
  fun shareHandsTheFaceToTheChooserAndCloses() {
    val face = solidJpeg(64, 64)
    var dismissed = 0
    compose.setContent {
      CabTheme(themeId = DEFAULT_THEME, fontId = DEFAULT_FONT) {
        AvatarSheet(slug = "kit", bytes = face, onReplace = {}, onDismiss = { dismissed++ })
      }
    }
    compose.onNodeWithText("Share").performClick()
    val chooser = shadowOf(app).nextStartedActivity!!
    assertEquals(Intent.ACTION_CHOOSER, chooser.action)
    val send = chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)!!
    assertEquals(Intent.ACTION_SEND, send.action)
    assertEquals("image/jpeg", send.type)
    assertEquals(1, dismissed)
  }

  /** The bundled default face is not the crane's work: nothing to copy out, only a photo to set. */
  @Test
  fun noFaceYetOffersOnlyReplace() {
    compose.setContent {
      CabTheme(themeId = DEFAULT_THEME, fontId = DEFAULT_FONT) {
        AvatarSheet(slug = "ada", bytes = null, onReplace = {}, onDismiss = {})
      }
    }
    compose.onNodeWithText("Ada's photo").assertIsDisplayed()
    compose.onNodeWithText("Copy").assertDoesNotExist()
    compose.onNodeWithText("Share").assertDoesNotExist()
    compose.onNodeWithText("Replace").assertIsDisplayed()
  }
}
