package com.gantree.cab

import com.gantree.cab.mailbox.PhoneContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
  fun repliesAreSpoken() {
    val app = CabApp()
    val frame = app.outbound("", null)
    assertTrue(frame.id!!.isNotEmpty())
    assertEquals("inbound", app.mouth.lines.value.single().kind)
    assertTrue(com.gantree.cab.mailbox.WireFrame(kind = "reply").spoken())
    assertFalse(com.gantree.cab.mailbox.WireFrame(kind = "reply", replay = true).spoken())
  }
}
