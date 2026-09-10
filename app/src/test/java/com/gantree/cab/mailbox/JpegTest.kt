package com.gantree.cab.mailbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JpegTest {
  @Test
  fun rejectsTinyHugeAndNonJpeg() {
    assertFalse(acceptJpeg(fakeJpeg(8)).ok)
    assertEquals("image too small", (acceptJpeg(fakeJpeg(8)) as JpegCheck.Err).detail)
    assertEquals("image too large (max 5MB)", (acceptJpeg(ByteArray(AVATAR_MAX_BYTES + 1)) as JpegCheck.Err).detail)
    val png = ByteArray(64)
    png[0] = 0x89.toByte()
    png[1] = 0x50
    assertFalse(acceptJpeg(png).ok)
    assertTrue(acceptJpeg(fakeJpeg()).ok)
  }

  @Test
  fun passthroughKeepsASmallJpeg() {
    assertTrue(shouldPassthroughJpeg("image/jpeg", 100, 64, 64))
    assertFalse(shouldPassthroughJpeg("image/png", 100, 64, 64))
    assertFalse(shouldPassthroughJpeg("image/jpeg", 100, 2000, 64))
    assertFalse(shouldPassthroughJpeg("image/jpeg", 2_000_000, 64, 64, maxBytes = 1_500_000))
  }
}

fun fakeJpeg(n: Int = 128): ByteArray {
  val b = ByteArray(n)
  b[0] = 0xFF.toByte()
  b[1] = 0xD8.toByte()
  b[2] = 0xFF.toByte()
  b[n - 1] = 0xD9.toByte()
  return b
}
