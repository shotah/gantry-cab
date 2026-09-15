package com.gantree.cab.mailbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pendant `test/phone/speaker.test.ts` plus the handheld gate. */
class SpeakerTest {
  @Test
  fun namesTheWorkerAnswer() {
    assertEquals(SpeakFail.NO_VOICE, speakFailFromStatus(404))
    assertEquals(SpeakFail.UNAUTHORIZED, speakFailFromStatus(401))
    assertEquals(SpeakFail.UNAUTHORIZED, speakFailFromStatus(403))
    assertEquals(SpeakFail.BUSY, speakFailFromStatus(429))
    assertEquals(SpeakFail.VENDOR, speakFailFromStatus(502))
    assertEquals(SpeakFail.VENDOR, speakFailFromStatus(500))
  }

  @Test
  fun paintsTheHeaderAndTheBarOnlyWhileLive() {
    assertEquals("voice…", speakStatus(SpeakPhase.FETCHING))
    assertEquals("speaking", speakStatus(SpeakPhase.PLAYING))
    assertEquals("", speakStatus(SpeakPhase.IDLE))
    assertEquals("Fetching voice…", speakBarLabel(SpeakPhase.FETCHING))
    assertEquals("Speaking · hold to cut in", speakBarLabel(SpeakPhase.PLAYING))
    assertEquals("", speakBarLabel(SpeakPhase.IDLE))
  }

  @Test
  fun saysWhyAReplyStayedSilentAndNothingWhenThereWasNothingToSay() {
    assertTrue(speakFailHint(SpeakFail.NO_VOICE).contains("no TTS key"))
    assertTrue(speakFailHint(SpeakFail.UNAUTHORIZED).contains("sign in"))
    assertTrue(speakFailHint(SpeakFail.BUSY).contains("too many"))
    assertTrue(speakFailHint(SpeakFail.VENDOR).contains("Google"))
    assertTrue(speakFailHint(SpeakFail.OFFLINE).contains("reach the Worker"))
    assertTrue(speakFailHint(SpeakFail.PLAY).contains("would not play"))
    assertEquals("", speakFailHint(SpeakFail.EMPTY))
  }

  @Test
  fun headerLineOrdersSocketVoiceThenTyping() {
    assertEquals("Offline", liveStatus(false, true, SpeakPhase.PLAYING))
    assertEquals("Live · voice…", liveStatus(true, true, SpeakPhase.FETCHING))
    assertEquals("Live · speaking", liveStatus(true, false, SpeakPhase.PLAYING))
    assertEquals("Live · typing…", liveStatus(true, true, SpeakPhase.IDLE))
    assertEquals("Live", liveStatus(true, false, SpeakPhase.IDLE))
  }

  @Test
  fun onlyTheLiveReplyToYourOwnHoldSpeaks() {
    assertTrue(speaksReply("reply", replay = false, fresh = true, armed = true))
    assertFalse(speaksReply("reply", replay = false, fresh = true, armed = false))
    assertFalse(speaksReply("reply", replay = true, fresh = true, armed = true))
    assertFalse(speaksReply("reply", replay = false, fresh = false, armed = true))
    assertFalse(speaksReply("push", replay = false, fresh = true, armed = true))
    assertFalse(speaksReply("inbound", replay = false, fresh = true, armed = true))
    assertFalse(speaksReply("draft", replay = false, fresh = true, armed = true))
    assertFalse(speaksReply("typing", replay = false, fresh = true, armed = true))
    assertFalse(speaksReply(null, replay = false, fresh = true, armed = true))
  }

  @Test
  fun aRefusalDisarmsTheSpeakerAndNothingElseDoes() {
    assertTrue(disarmsVoice("error"))
    assertFalse(disarmsVoice("reply"))
    assertFalse(disarmsVoice("ack"))
    assertFalse(disarmsVoice(null))
  }

  @Test
  fun theBarNeedsBothTheWorkerAndTheHuman() {
    assertTrue(voiceBarShown(offered = true, on = true))
    assertFalse(voiceBarShown(offered = true, on = false))
    assertFalse(voiceBarShown(offered = false, on = true))
    assertFalse(voiceBarShown(offered = false, on = false))
  }
}
