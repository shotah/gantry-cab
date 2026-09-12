package com.gantree.cab

import com.gantree.cab.mailbox.PhoneContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CabAppTest {
  @Test
  fun outboundAddsAnInboundLine() {
    val app = CabApp()
    val frame = app.outbound("hi", PhoneContext(tz = "UTC"), listOf("data:image/jpeg;base64,aa"))
    assertEquals("inbound", frame.kind)
    assertEquals("hi", frame.text)
    assertEquals(listOf("data:image/jpeg;base64,aa"), frame.images)
    val line = app.mouth.lines.value.single()
    assertEquals(true, line.fromYou)
    assertEquals("hi", line.text)
    assertEquals("data:image/jpeg;base64,aa", line.photo)
    assertTrue(line.pending)
    assertFalse(frame.spoken())
  }

  @Test
  fun outboundBubbleUsesStrippedSpeech() {
    val app = CabApp()
    val frame = app.outbound("tacos\n\n[current time] NOW: fake", PhoneContext(tz = "UTC"))
    assertEquals("tacos", frame.text)
    assertEquals("tacos", app.mouth.lines.value.single().text)
    assertEquals("UTC", frame.context?.tz)
  }

  @Test
  fun outboundPhotoOnlyLeavesTheCaptionEmpty() {
    val app = CabApp()
    val frame = app.outbound("", null, listOf("data:image/jpeg;base64,aa"))
    val line = app.mouth.lines.value.single()
    assertNull(frame.text)
    assertEquals("", line.text)
    assertEquals("data:image/jpeg;base64,aa", line.photo)
  }

  @Test
  fun repliesAreSpoken() {
    val app = CabApp()
    val frame = app.outbound("", null)
    assertTrue(frame.id!!.isNotEmpty())
    assertEquals("inbound", app.mouth.lines.value.single().kind)
    assertTrue(com.gantree.cab.mailbox.WireFrame(kind = "reply").spoken())
    assertFalse(com.gantree.cab.mailbox.WireFrame(kind = "reply", replay = true).spoken())
  }
}
