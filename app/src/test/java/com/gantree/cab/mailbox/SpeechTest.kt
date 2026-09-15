package com.gantree.cab.mailbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pendant `test/phone/speech.test.ts` `spokenFrom` + `listen` cases, on the recognizer-free half. */
class SpeechTest {
  @Test
  fun keepsFinalsAndTheTrailingInterimSoAHungStopStillHasWords() {
    assertEquals("how is it going", spokenFrom(listOf("how is"), "it going"))
    assertEquals("guess", spokenFrom(emptyList(), "guess"))
    assertEquals("done really", spokenFrom(listOf("done", "really")))
    assertEquals("", spokenFrom(emptyList()))
    assertEquals("", spokenFrom(listOf("  "), " "))
  }

  @Test
  fun collapsesGrowingFinalsSoAHoldIsOneSentenceNotAStutter() {
    assertEquals(
      "well I can send you a message but it's not reading it aloud back to me",
      spokenFrom(
        listOf(
          "well",
          "well I",
          "well I can send",
          "well I can send you a message but it's not reading it aloud back to me",
        ),
      ),
    )
    assertEquals("well I can send you a message", spokenFrom(listOf("well"), "well I can send you a message"))
    assertEquals("yes yesterday", spokenFrom(listOf("yes", "yesterday")))
    assertEquals("turn left at the light", spokenFrom(listOf("turn left", "at the light")))
    assertEquals("Turn left.", spokenFrom(listOf("turn left", "Turn left.")))
    assertEquals("turn left", spokenFrom(listOf("turn left", "turn")))
  }

  @Test
  fun growsSpokenNeedsAWordBoundaryAndIgnoresCaseAndRuns() {
    assertTrue(growsSpoken("well", "well I"))
    assertTrue(growsSpoken("well", "Well,  I"))
    assertTrue(growsSpoken("hi", "hi"))
    assertFalse(growsSpoken("yes", "yesterday"))
    assertFalse(growsSpoken("", "x"))
    assertFalse(growsSpoken("x", ""))
    assertFalse(growsSpoken("well I", "well"))
  }

  @Test
  fun utteranceHandsBackTheWordsOnceOnFinish() {
    val got = mutableListOf<String>()
    val u = Utterance { got.add(it) }
    u.final("where is")
    u.final("the nearest gas")
    u.finish()
    u.finish()
    assertEquals(listOf("where is the nearest gas"), got)
    assertTrue(u.ended)
  }

  @Test
  fun utteranceSendsTheInterimGuessWhenAFinalNeverLanded() {
    val got = mutableListOf<String>()
    val u = Utterance { got.add(it) }
    u.partial("how is")
    u.partial("how is it doing")
    u.finish()
    assertEquals(listOf("how is it doing"), got)
  }

  @Test
  fun aFinalClearsTheInterimAndALaterPartialRidesAfterIt() {
    val u = Utterance { }
    u.partial("x")
    u.final("done")
    assertEquals("done", u.heard)
    u.partial("rea")
    assertEquals("done rea", u.heard)
    u.final("really")
    assertEquals("done really", u.heard)
  }

  @Test
  fun abortDropsWhatWasHeardAndNothingLandsAfterwards() {
    val got = mutableListOf<String>()
    val u = Utterance { got.add(it) }
    u.final("that was the radio")
    u.abort()
    u.final("late")
    u.partial("later")
    u.finish()
    assertEquals(listOf(""), got)
    assertEquals("", u.heard)
  }

  @Test
  fun holdLabelsMatchThePendantBar() {
    assertEquals("Hold to talk", holdLabel(HoldState.IDLE))
    assertEquals("Release to send", holdLabel(HoldState.LISTENING))
    assertEquals("…", holdLabel(HoldState.FINISHING))
    assertEquals("Mic blocked", holdLabel(HoldState.BLOCKED))
    assertEquals(2_000L, LISTEN_END_MS)
  }
}
