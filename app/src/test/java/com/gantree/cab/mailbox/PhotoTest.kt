package com.gantree.cab.mailbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoTest {
  @Test
  fun smallJpegBecomesADataUrl() {
    val got = photoDataUrl(byteArrayOf(1, 2, 3), "image/jpg")
    assertTrue(got.ok)
    val url = (got as PhotoResult.Ok).url
    assertTrue(url.startsWith("data:image/jpeg;base64,"))
    assertTrue(decodeDataUrl(url)!!.contentEquals(byteArrayOf(1, 2, 3)))
  }

  @Test
  fun rejectsEmptyHugeAndNonImage() {
    assertEquals("bad photo", (photoDataUrl(byteArrayOf(), "image/jpeg") as PhotoResult.Err).error)
    assertEquals("bad photo", (photoDataUrl(byteArrayOf(1), "text/plain") as PhotoResult.Err).error)
    assertEquals("too large", (photoDataUrl(ByteArray(IMAGE_BYTES_MAX + 1), "image/jpeg") as PhotoResult.Err).error)
    assertTrue(parsePhotoFile("image/png", 10).ok)
    assertFalse(parsePhotoFile("application/pdf", 10).ok)
    assertFalse(parsePhotoFile("image/jpeg", 0).ok)
    assertEquals("too large", (parsePhotoFile("image/jpeg", IMAGE_BYTES_MAX + 1) as PhotoResult.Err).error)
  }

  @Test
  fun decodeRequiresADataImage() {
    assertNull(decodeDataUrl("https://example.test/a.jpg"))
    assertNull(decodeDataUrl("data:text/plain;base64,YQ=="))
    assertNull(decodeDataUrl("data:image/jpeg;base64,!!!!"))
    val wrapped = "data:image/png;base64,\nAQID"
    assertTrue(decodeDataUrl(wrapped)!!.contentEquals(byteArrayOf(1, 2, 3)))
  }
}
