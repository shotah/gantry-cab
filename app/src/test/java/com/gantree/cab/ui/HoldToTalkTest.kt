package com.gantree.cab.ui

import android.app.Application
import android.content.ComponentName
import android.content.IntentFilter
import android.os.Bundle
import android.os.Looper
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.gantree.cab.mailbox.HoldState
import com.gantree.cab.mailbox.LISTEN_END_MS
import com.gantree.cab.mailbox.SpeakPhase
import com.gantree.cab.mailbox.holdLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSpeechRecognizer
import java.util.concurrent.TimeUnit

/**
 * A finger on the hold bar, through Compose pointer input, to Android's
 * [SpeechRecognizer] and back out as words. The recognizer is Robolectric's
 * shadow; the touch is real. Pendant `test/app/components/chat/HoldToTalk`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class HoldToTalkTest {
  @get:Rule
  val compose = createComposeRule()

  private val sent = mutableListOf<String>()
  private var holds = 0
  private var micAsks = 0

  @Before
  fun phoneHasARecognizer() {
    // `SpeechRecognizer.isRecognitionAvailable` is a PackageManager query for the service.
    val app: Application = ApplicationProvider.getApplicationContext()
    val engine = ComponentName("com.example.stt", "com.example.stt.Engine")
    shadowOf(app.packageManager).addServiceIfNotPresent(engine)
    shadowOf(app.packageManager).addIntentFilterForService(engine, IntentFilter(RecognitionService.SERVICE_INTERFACE))
    ShadowSpeechRecognizer.reset()
  }

  private fun bar(micGranted: Boolean = true, disabled: Boolean = false) {
    compose.setContent {
      HoldToTalk(
        disabled = disabled,
        micGranted = micGranted,
        speaking = SpeakPhase.IDLE,
        onMicAsk = { micAsks++ },
        onHoldStart = { holds++ },
        onText = { sent += it },
      )
    }
  }

  private fun words(text: String): Bundle =
    Bundle().apply { putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf(text)) }

  private fun recognizer(): ShadowSpeechRecognizer =
    shadowOf(ShadowSpeechRecognizer.getLatestSpeechRecognizer() ?: error("no recognizer started"))

  private fun idle() {
    shadowOf(Looper.getMainLooper()).idle()
    compose.waitForIdle()
  }

  @Test
  fun pressListensAndReleaseSendsTheFinal() {
    bar()
    compose.onNodeWithContentDescription("Hold to talk").performTouchInput { down(center) }
    idle()

    assertEquals(1, holds)
    compose.onNodeWithText(holdLabel(HoldState.LISTENING)).assertIsDisplayed()
    val intent = recognizer().lastRecognizerIntent
    assertTrue(intent.getBooleanExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false))
    recognizer().triggerOnPartialResults(words("hello"))

    compose.onNodeWithContentDescription("Hold to talk").performTouchInput { up() }
    idle()
    compose.onNodeWithText(holdLabel(HoldState.FINISHING)).assertIsDisplayed()

    recognizer().triggerOnResults(words("hello there"))
    idle()

    assertEquals(listOf("hello there"), sent)
    compose.onNodeWithText(holdLabel(HoldState.IDLE)).assertIsDisplayed()
    assertTrue(recognizer().isDestroyed)
  }

  @Test
  fun releaseWithoutAFinalCommitsWhatWasHeardOnTheWatchdog() {
    bar()
    compose.onNodeWithContentDescription("Hold to talk").performTouchInput { down(center) }
    idle()
    recognizer().triggerOnPartialResults(words("hello"))
    compose.onNodeWithContentDescription("Hold to talk").performTouchInput { up() }
    idle()

    assertEquals(emptyList<String>(), sent)
    shadowOf(Looper.getMainLooper()).idleFor(LISTEN_END_MS + 1, TimeUnit.MILLISECONDS)
    compose.waitForIdle()

    assertEquals(listOf("hello"), sent)
  }

  @Test
  fun slideOffThrowsTheWordsAway() {
    bar()
    compose.onNodeWithContentDescription("Hold to talk").performTouchInput { down(center) }
    idle()
    recognizer().triggerOnPartialResults(words("that was the radio"))

    compose.onNodeWithContentDescription("Hold to talk").performTouchInput {
      moveTo(Offset(center.x, -400f))
      up()
    }
    idle()

    assertEquals(emptyList<String>(), sent)
    assertTrue(recognizer().isDestroyed)
    compose.onNodeWithText(holdLabel(HoldState.IDLE)).assertIsDisplayed()
  }

  @Test
  fun pauseMidHoldKeepsListening() {
    bar()
    compose.onNodeWithContentDescription("Hold to talk").performTouchInput { down(center) }
    idle()
    // Google's engine ends the utterance on silence; still held, so it is started again.
    recognizer().triggerOnResults(words("first part"))
    idle()
    compose.onNodeWithText(holdLabel(HoldState.LISTENING)).assertIsDisplayed()
    assertEquals(emptyList<String>(), sent)

    compose.onNodeWithContentDescription("Hold to talk").performTouchInput { up() }
    idle()
    recognizer().triggerOnResults(words("second part"))
    idle()

    assertEquals(listOf("first part second part"), sent)
  }

  @Test
  fun noMicAsksInsteadOfListening() {
    bar(micGranted = false)
    compose.onNodeWithContentDescription("Hold to talk").performTouchInput { down(center) }
    idle()

    assertEquals(1, micAsks)
    assertEquals(0, holds)
    assertNull(ShadowSpeechRecognizer.getLatestSpeechRecognizer())
    compose.onNodeWithText(holdLabel(HoldState.BLOCKED)).assertIsDisplayed()

    compose.onNodeWithContentDescription("Hold to talk").performTouchInput { up() }
    idle()
    assertEquals(emptyList<String>(), sent)
  }

  @Test
  fun socketDownIgnoresThePress() {
    bar(disabled = true)
    compose.onNodeWithContentDescription("Hold to talk").performTouchInput { down(center) }
    idle()

    assertEquals(0, holds)
    assertNull(ShadowSpeechRecognizer.getLatestSpeechRecognizer())
  }
}
