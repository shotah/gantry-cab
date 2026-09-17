package com.gantree.cab.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.gantree.cab.ChatLine
import com.gantree.cab.mailbox.DEFAULT_FONT
import com.gantree.cab.mailbox.DEFAULT_THEME
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ChatTurnTest {
  @get:Rule
  val compose = createComposeRule()

  @Test
  fun chipOpensThePaletteAndAPickReportsTheEmoji() {
    var picked = ""
    compose.setContent {
      var picking by remember { mutableStateOf(false) }
      CabTheme(themeId = DEFAULT_THEME, fontId = DEFAULT_FONT) {
        ChatTurn(
          line = ChatLine("r1", false, "latched", "reply", reaction = "👍"),
          reactable = true,
          picking = picking,
          onOpenPicker = { picking = true },
          onPick = {
            picked = it
            picking = false
          },
        )
      }
    }
    compose.onNodeWithContentDescription("reaction 👍").assertIsDisplayed().performClick()
    compose.onNodeWithContentDescription("React ❤️").performClick()
    assertEquals("❤️", picked)
  }
}
