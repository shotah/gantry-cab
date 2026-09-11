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
  fun jpegBudgetMatchesPendantAndKeepsTheDataUrlUnderTheWireCap() {
    // pendant lib/phone/photo.ts: floor((1_500_000 - 32) / 4) * 3
    assertEquals(1_124_976, PHOTO_JPEG_BYTES_MAX)
    val fits = photoDataUrl(ByteArray(PHOTO_JPEG_BYTES_MAX), "image/jpeg")
    assertTrue(fits.ok)
    assertTrue((fits as PhotoResult.Ok).url.toByteArray(Charsets.UTF_8).size <= IMAGE_BYTES_MAX)
    val over = photoDataUrl(ByteArray(PHOTO_JPEG_BYTES_MAX + 1), "image/jpeg")
    assertEquals("too large", (over as PhotoResult.Err).error)
  }

  @Test
  fun photoSizesMatchThePendantTable() {
    assertEquals(listOf("full", "medium", "small"), PHOTO_SIZE_IDS)
    assertEquals("medium", DEFAULT_PHOTO_SIZE)
    assertEquals(DEFAULT_PHOTO_SIZE, parsePhotoSize(null))
    assertEquals("medium", parsePhotoSize("nope"))
    assertEquals("small", parsePhotoSize("small"))
    assertEquals(CHAT_PHOTO_EDGE, photoEdge("full"))
    assertEquals(1024, photoEdge("medium"))
    assertEquals(640, photoEdge("small"))
    assertEquals(1024, photoEdge(null))
    assertEquals("Full", photoSizeLabel("full"))
    assertEquals("Medium", photoSizeLabel("nope"))
    assertEquals("Small · 640 px", photoSizeChip("small"))
  }

  @Test
  fun ladderStepsQualityDownThenTheEdgeUntilTheFloor() {
    assertEquals(listOf(90, 80, 70, 60), JPEG_QUALITY_STEPS)
    val steps = shrinkSteps(1600, 4000)
    assertEquals(JpegStep(1600, 90), steps.first())
    assertEquals(listOf(90, 80, 70, 60), steps.filter { it.edge == 1600 }.map { it.quality })
    assertEquals(listOf(1600, 1200, 900, 675, 506, 380), steps.map { it.edge }.distinct())
    val smallest = steps.last().edge
    assertTrue(smallest >= JPEG_EDGE_MIN)
    assertTrue(smallest < 480)
    assertEquals(JpegStep(380, 60), steps.last())
  }

  @Test
  fun ladderNeverUpscalesASmallImage() {
    val steps = shrinkSteps(1600, 1000)
    assertEquals(JpegStep(1000, 90), steps.first())
    assertEquals(listOf(1000, 750, 563, 422), steps.map { it.edge }.distinct())
  }

  @Test
  fun shrinkToFitTakesTheFirstEncodeUnderBudgetOrGivesUp() {
    val tried = mutableListOf<JpegStep>()
    // A 4:3 camera shot: bytes scale with pixels and quality, like the pendant test.
    val got = shrinkToFit(shrinkSteps(1600, 4000), 300_000) { step ->
      tried += step
      val h = step.edge * 3 / 4
      ByteArray(step.edge * h * step.quality / 100 * 3 / 10)
    }
    assertTrue(got!!.size <= 300_000)
    assertEquals(listOf(90, 80, 70, 60), tried.filter { it.edge == 1600 }.map { it.quality })
    assertEquals(listOf(1600, 1200), tried.map { it.edge }.distinct())
    assertEquals(1200, tried.last().edge)
    assertNull(shrinkToFit(shrinkSteps(1600, 4000), 1_000) { ByteArray(500_000) })
    assertNull(shrinkToFit(emptyList(), 10) { ByteArray(1) })
  }

  @Test
  fun decodeRequiresADataImage() {
    assertNull(decodeDataUrl("https://example.test/a.jpg"))
    assertNull(decodeDataUrl("data:text/plain;base64,YQ=="))
    assertNull(decodeDataUrl("data:image/jpeg;base64,!!!!"))
    val wrapped = "data:image/png;base64,\nAQID"
    assertTrue(decodeDataUrl(wrapped)!!.contentEquals(byteArrayOf(1, 2, 3)))
    val huge = "data:image/jpeg;base64," + "A".repeat(2_100_000)
    assertNull(decodeDataUrl(huge))
  }
}
