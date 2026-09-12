package com.gantree.cab.drive

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KitFaceTest {
  @Test
  fun missingOrJunkJpegIsNoFace() {
    assertNull(kitFaceBitmap(null))
    assertNull(kitFaceBitmap(ByteArray(0)))
    assertNull(kitFaceBitmap(ByteArray(16) { 1 }))
    assertNull(kitFaceIcon(null))
  }

  @Test
  fun jpegBecomesASquareBitmapAutoCanPaint() {
    val bmp = kitFaceBitmap(solidJpeg(80, 40)) ?: error("expected a face bitmap")
    assertEquals(KIT_FACE_EDGE, bmp.width)
    assertEquals(KIT_FACE_EDGE, bmp.height)
    assertNotNull(kitFaceIcon(solidJpeg()))
  }

  @Test
  fun kitPersonKeepsTheSlugAndDropsAMissingIcon() {
    val bare = kitPerson("kit")
    assertEquals("kit", bare.name.toString())
    assertEquals("cab-kit", bare.key)
    assertNull(bare.icon)
    assertNotNull(kitPerson("kit", kitFaceIcon(solidJpeg())).icon)
  }
}

fun solidJpeg(w: Int = 64, h: Int = 64, color: Int = 0xFFCC5533.toInt()): ByteArray {
  val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
  bmp.eraseColor(color)
  val out = ByteArrayOutputStream()
  check(bmp.compress(CompressFormat.JPEG, 90, out))
  return out.toByteArray()
}
