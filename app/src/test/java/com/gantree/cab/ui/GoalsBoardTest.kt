package com.gantree.cab.ui

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.gantree.cab.mailbox.DEFAULT_FONT
import com.gantree.cab.mailbox.DEFAULT_THEME
import com.gantree.cab.mailbox.parseAims
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class GoalsBoardTest {
  @get:Rule
  val compose = createComposeRule()

  private val board = parseAims(
    JSONObject(
      """{"aims":[
        {"area":"training","sentence":"gym 3 mornings/wk","rating30":1.4,"sum7":6,"streak":2,"note":"asked",
         "days":[{"day":"2026-09-22","score":2,"events":[411]},{"day":"2026-09-23","score":0,"events":[]}],
         "weeks":[{"start":"2026-09-13","mean":0.9,"up":3,"against":1},{"start":"2026-09-20","mean":-0.4,"up":1,"against":2}],
         "slope":0.3},
        {"area":"weight","sentence":"under 190","rating30":-0.5,"sum7":-2,"streak":0,"note":"","days":[]}
      ],"links":[{"a":"training","b":"weight","r":0.38,"n":12}]}""",
    ),
  )!!

  @Test
  fun buttonHiddenWhenTheBoardIsEmptyAndCountsWhenNot() {
    var opened = 0
    compose.setContent {
      CabTheme(themeId = DEFAULT_THEME, fontId = DEFAULT_FONT) {
        GoalsButton(count = 0, onOpen = { opened++ })
        GoalsButton(count = 2, onOpen = { opened++ })
      }
    }
    compose.onNodeWithContentDescription("goals (0)").assertDoesNotExist()
    compose.onNodeWithContentDescription("goals (2)").assertIsDisplayed().performClick()
    assertEquals(1, opened)
  }

  @Test
  fun everyButtonIsAVisibleTurnAndClosesTheSheet() {
    val asked = mutableListOf<String>()
    var dismissed = 0
    compose.setContent {
      CabTheme(themeId = DEFAULT_THEME, fontId = DEFAULT_FONT) {
        GoalsSheet(board = board, onAsk = { asked += it }, onDismiss = { dismissed++ })
      }
    }
    // The sheet scrolls and the test screen is short: bring each line up before reading it.
    compose.onNodeWithText("gym 3 mornings/wk").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("30d +1.4 · 7d +6 · streak 2 · asked").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("slope +0.3/wk").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("training → next-day weight r +0.38 (n 12)").performScrollTo().assertIsDisplayed()
    compose.onNodeWithContentDescription("Ask Kit about training").performScrollTo().performClick()
    compose.onNodeWithText("Full report").performScrollTo().performClick()
    compose.onNodeWithText("Rubric").performScrollTo().performClick()
    assertEquals(listOf("/aims training", "/aims", "/aims rubric"), asked)
    assertEquals(3, dismissed)
  }

  @Test
  fun scoreHueAndWeight() {
    val up = Color.Green
    val against = Color.Red
    val zero = Color.Gray
    assertEquals(up, scoreColor(1.4, up, against, zero))
    assertEquals(against, scoreColor(-0.1, up, against, zero))
    assertEquals(zero, scoreColor(0.0, up, against, zero))
    assertTrue(dayWeight(1) < dayWeight(2))
    assertTrue(dayWeight(2) < dayWeight(3))
    assertEquals(dayWeight(3), dayWeight(9), 0f)
    assertEquals(1f, dayWeight(3), 0.001f)
  }
}
