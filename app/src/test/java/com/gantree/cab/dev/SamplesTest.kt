package com.gantree.cab.dev

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SamplesTest {
  @Test
  fun parseSampleAcceptsKnownIds() {
    assertEquals("thread", parseSample("Thread"))
    assertNull(parseSample("nope"))
    assertNull(parseSample(null))
  }

  @Test
  fun threadSceneMatchesPendantCopy() {
    val scene = sampleScene("thread")!!
    assertTrue(scene.up)
    assertEquals(4, scene.lines.size)
    assertEquals("On the dock — is the gate still open?", scene.lines.first().text)
    assertEquals("Leave-by 20:50. Pin is this-send, ±12m.", scene.lines.last().text)
  }

  @Test
  fun remainingScenesCoverTheDoors() {
    val unsigned = sampleScene("unsigned")!!
    assertEquals("", unsigned.email)
    assertEquals(false, unsigned.up)
    assertTrue(unsigned.lines.isEmpty())

    val empty = sampleScene("empty")!!
    assertEquals(true, empty.up)
    assertTrue(empty.lines.isEmpty())

    val ping = sampleScene("ping")!!
    assertEquals("push", ping.lines.first().kind)
    assertEquals(3, ping.lines.size)

    val down = sampleScene("down")!!
    assertEquals(false, down.up)
    assertEquals(1, down.lines.size)

    val stream = sampleScene("stream")!!
    assertEquals(true, stream.up)
    assertEquals(true, stream.typing)
    assertEquals("draft", stream.lines.last().kind)
    assertEquals("__draft__", stream.lines.last().id)

    val photo = sampleScene("photo")!!
    assertEquals(true, photo.lines.first().photo!!.startsWith("data:image/jpeg"))
    assertEquals("This the right hatch?", photo.lines.first().text)
  }
}
