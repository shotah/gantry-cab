package com.gantree.cab.ui

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.gantree.cab.mailbox.DEFAULT_FONT
import com.gantree.cab.mailbox.DEFAULT_THEME
import com.gantree.cab.mailbox.TodoRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TasksBoardTest {
  @get:Rule
  val compose = createComposeRule()

  private val today = LocalDate.parse("2026-09-26")
  private val rows = listOf(
    TodoRow(412, "dentist", "call to book a cleaning", "2026-09-23"),
    TodoRow(418, "passport", "renew, by Oct 15", "2026-09-26"),
  )

  @Test
  fun buttonHiddenWhenEmptyBareWhenSeenAndBadgedOnlyForChanges() {
    var opened = 0
    compose.setContent {
      CabTheme(themeId = DEFAULT_THEME, fontId = DEFAULT_FONT) {
        TasksButton(shown = false, changed = 3, onOpen = { opened++ })
        TasksButton(shown = true, changed = 0, onOpen = { opened++ })
        TasksButton(shown = true, changed = 2, onOpen = { opened++ })
      }
    }
    compose.onNodeWithContentDescription("tasks (3)").assertDoesNotExist()
    compose.onNodeWithContentDescription("tasks").assertIsDisplayed()
    compose.onNodeWithContentDescription("tasks (2)").assertIsDisplayed().performClick()
    compose.onNodeWithText("2").assertIsDisplayed()
    assertEquals(1, opened)
  }

  @Test
  fun aTickSendsOnceAndLeavesTheSheetOpen() {
    val sent = mutableListOf<String>()
    var dismissed = 0
    compose.setContent {
      CabTheme(themeId = DEFAULT_THEME, fontId = DEFAULT_FONT) {
        TasksSheet(
          rows = rows,
          onTick = { sent += it },
          onAsk = { sent += it },
          onDismiss = { dismissed++ },
          today = today,
        )
      }
    }
    compose.onNodeWithText("call to book a cleaning").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("#412 · dentist · 3d ago").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("#418 · passport").performScrollTo().assertIsDisplayed()
    compose.onNodeWithContentDescription("done dentist").performScrollTo().performClick()
    compose.onNodeWithContentDescription("done dentist").performClick()
    assertEquals(listOf("/todo done 412"), sent)
    assertEquals(0, dismissed)
  }

  @Test
  fun addAndFullListAreTurnsThatCloseTheSheet() {
    val sent = mutableListOf<String>()
    var dismissed = 0
    compose.setContent {
      CabTheme(themeId = DEFAULT_THEME, fontId = DEFAULT_FONT) {
        TasksSheet(
          rows = rows,
          onTick = {},
          onAsk = { sent += it },
          onDismiss = { dismissed++ },
          today = today,
        )
      }
    }
    compose.onNodeWithText("in your words").performScrollTo().performTextInput("book a cleaning")
    compose.onNodeWithText("Add").performScrollTo().performClick()
    compose.onNodeWithText("Full list").performScrollTo().performClick()
    assertEquals(listOf("add to my list: book a cleaning", "/todo"), sent)
    assertEquals(2, dismissed)
  }

  /** `!!` and `!` sort to the top and paint as a tag, not as the first word of the task. */
  @Test
  fun urgentThenHighLeadTheSheetWithTheMarkerAsATag() {
    val ranked = listOf(
      TodoRow(412, "dentist", "call to book a cleaning", "2026-09-23"),
      TodoRow(418, "passport", "! renew, by Oct 15", "2026-09-24"),
      TodoRow(421, "taxes", "!! file the extension", "2026-09-25"),
    )
    compose.setContent {
      CabTheme(themeId = DEFAULT_THEME, fontId = DEFAULT_FONT) {
        TasksSheet(rows = ranked, onTick = {}, onAsk = {}, onDismiss = {}, today = today)
      }
    }
    val taxes = compose.onNodeWithText("file the extension").performScrollTo().getBoundsInRoot()
    val passport = compose.onNodeWithText("renew, by Oct 15").performScrollTo().getBoundsInRoot()
    val dentist = compose.onNodeWithText("call to book a cleaning").performScrollTo().getBoundsInRoot()
    assertTrue(taxes.top < passport.top)
    assertTrue(passport.top < dentist.top)
    compose.onNodeWithContentDescription("urgent").assertIsDisplayed()
    compose.onNodeWithContentDescription("high").assertIsDisplayed()
    compose.onNodeWithText("!! file the extension").assertDoesNotExist()
  }

  @Test
  fun footerAppearsPastTen() {
    val many = (1..11).map { TodoRow(it.toLong(), "t$it", "do $it", "2026-09-26") }
    compose.setContent {
      CabTheme(themeId = DEFAULT_THEME, fontId = DEFAULT_FONT) {
        TasksSheet(rows = many, onTick = {}, onAsk = {}, onDismiss = {}, today = today)
      }
    }
    compose.onNodeWithText("11 open — a pocket list; prune, or use a tracker")
      .performScrollTo()
      .assertIsDisplayed()
  }
}
