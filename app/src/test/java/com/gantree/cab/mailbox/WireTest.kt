package com.gantree.cab.mailbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WireTest {
  @Test
  fun inboundRoundTripKeepsTextKindAndGeo() {
    val frame = inbound(
      "near me",
      "id-1",
      PhoneContext(
        at = "2026-09-09T12:00:00.000Z",
        tz = "America/Los_Angeles",
        geo = Geo(lat = 47.6, lon = -122.3, accuracyM = 12.0),
      ),
    )
    val raw = encodeFrame(frame)
    val got = parseFrame(raw)!!
    assertEquals("near me", got.text)
    assertEquals("inbound", got.kind)
    assertEquals("id-1", got.id)
    assertTrue(raw.contains("47.6"))
    assertFalse(raw.contains("[location]"))
  }

  @Test
  fun ackSinceIsTheReconnectCatchUp() {
    val got = parseFrame(encodeFrame(ackSince("abc")))
    assertEquals("ack", got?.kind)
    assertEquals("abc", got?.since)
  }

  @Test
  fun kitReplyAndCronAreSpokenInTheCar() {
    assertTrue(shouldSpeak("reply"))
    assertTrue(shouldSpeak("push"))
    assertFalse(shouldSpeak("ack"))
    assertFalse(shouldSpeak("inbound"))
  }

  @Test
  fun junkAndKeepaliveAreDropped() {
    assertNull(parseFrame("nope"))
    assertNull(parseFrame("ping"))
    assertNull(parseFrame("pong"))
  }
}
