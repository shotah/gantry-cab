package com.gantree.cab.mailbox

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import com.gantree.cab.drive.solidJpeg
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.File

/**
 * The photo picker hands back a `content://` URI; [jpegFromUri] is the only
 * thing between that and the wire. NATIVE graphics runs the real Skia
 * decoder: `inJustDecodeBounds` returns null, junk fails, EXIF is honoured.
 * Legacy shadows fake all three and would pass a broken reader.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class JpegIoTest {
  private val app: Context = ApplicationProvider.getApplicationContext()
  private val resolver: ContentResolver = app.contentResolver

  @Before
  fun freshProvider() = resetFileProvider()

  /** A picker-style `content://` URI; not the `media` authority, which Robolectric fakes. */
  private fun picked(name: String, bytes: ByteArray): Uri {
    val uri = Uri.parse("content://com.gantree.cab.test.picker/media/$name")
    shadowOf(resolver).registerInputStreamSupplier(uri) { ByteArrayInputStream(bytes) }
    return uri
  }

  private fun bounds(jpeg: ByteArray): Pair<Int, Int> {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, opts)
    return opts.outWidth to opts.outHeight
  }

  @Test
  fun keepsAPickedJpegThatAlreadyFits() {
    val raw = solidJpeg(64, 64)
    val got = jpegFromUri(resolver, picked("small.jpg", raw), edge = 1024, maxBytes = PHOTO_JPEG_BYTES_MAX)
    assertArrayEquals(raw, got)
  }

  @Test
  fun shrinksABigPhotoToTheChosenEdge() {
    val raw = solidJpeg(2000, 1000)
    val got = jpegFromUri(resolver, picked("big.jpg", raw), edge = 640, maxBytes = PHOTO_JPEG_BYTES_MAX)
    assertTrue(acceptJpeg(got).ok)
    val (w, h) = bounds(got)
    assertEquals(640, maxOf(w, h))
    assertTrue(got.size <= PHOTO_JPEG_BYTES_MAX)
  }

  @Test
  fun keepsAPortraitCameraShotUprightWhenShrinking() {
    // Camera apps store the sensor frame landscape and set EXIF orientation 6 (rotate 90°).
    val raw = withExifOrientation(solidJpeg(80, 40), 6)
    val got = jpegFromUri(resolver, picked("portrait.jpg", raw), edge = 32, maxBytes = PHOTO_JPEG_BYTES_MAX)
    val (w, h) = bounds(got)
    assertEquals(32, maxOf(w, h))
    assertTrue("expected portrait, got ${w}x$h", h > w)
  }

  @Test
  fun junkBytesAreCouldNotRead() {
    val e = assertThrows(IllegalStateException::class.java) {
      jpegFromUri(resolver, picked("junk.bin", ByteArray(16) { 1 }), edge = 640, maxBytes = PHOTO_JPEG_BYTES_MAX)
    }
    assertEquals("could not read that image", e.message)
  }

  @Test
  fun tooSmallABudgetIsImageTooLarge() {
    val raw = solidJpeg(2000, 1000)
    val e = assertThrows(IllegalStateException::class.java) {
      jpegFromUri(resolver, picked("huge.jpg", raw), edge = 1600, maxBytes = 1)
    }
    assertEquals("image too large", e.message)
  }

  @Test
  fun cameraShotIsAFileProviderUriInPrivateCache() {
    // Throws unless the manifest declares the authority and camera_paths.xml covers the folder.
    val uri = cameraShotUri(app)
    assertEquals("content", uri.scheme)
    assertEquals("${app.packageName}.fileprovider", uri.authority)
    val file = File(app.cacheDir, "camera/shot.jpg")
    assertTrue(file.parentFile!!.isDirectory)
    // Same URI every call: TakePicture's callback rebuilds it after a process restart.
    assertEquals(uri, cameraShotUri(app))
    // What the camera app writes there is what jpegFromUri reads back.
    val raw = solidJpeg(64, 64)
    file.writeBytes(raw)
    assertArrayEquals(raw, jpegFromUri(resolver, uri, edge = 1024, maxBytes = PHOTO_JPEG_BYTES_MAX))
  }

  @Test
  fun faceOutIsAFileProviderUriHoldingTheBytes() {
    val raw = solidJpeg(64, 64)
    val uri = avatarShareUri(app, raw)
    assertEquals("content", uri.scheme)
    assertEquals("${app.packageName}.fileprovider", uri.authority)
    assertArrayEquals(raw, resolver.openInputStream(uri)!!.use { it.readBytes() })
    // The next copy overwrites the same file; nothing piles up in cache.
    val next = solidJpeg(32, 32)
    assertEquals(uri, avatarShareUri(app, next))
    assertArrayEquals(next, resolver.openInputStream(uri)!!.use { it.readBytes() })
  }

  @Test
  fun shareImageIntentIsAChooserOverSendWithAReadGrant() {
    val uri = Uri.parse("content://com.gantree.cab.fileprovider/avatar/face.jpg")
    val chooser = shareImageIntent(uri)
    assertEquals(Intent.ACTION_CHOOSER, chooser.action)
    val send = chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)!!
    assertEquals(Intent.ACTION_SEND, send.action)
    assertEquals("image/jpeg", send.type)
    assertEquals(uri, send.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
    assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
  }
}

/**
 * [FileProvider] caches its path roots per authority in a static map.
 * Robolectric gives every test method a fresh data dir but keeps the class
 * loader, so the second test to mint a URI would miss the roots the first one
 * cached ("Failed to find configured root"). Call from `@Before`.
 */
fun resetFileProvider() {
  val field = FileProvider::class.java.getDeclaredField("sCache").apply { isAccessible = true }
  (field.get(null) as MutableMap<*, *>).clear()
}

/** Splice a minimal Exif APP1 (one IFD0 entry: 0x0112 Orientation) right after SOI. */
fun withExifOrientation(jpeg: ByteArray, orientation: Int): ByteArray {
  check(jpeg.size > 2 && jpeg[0] == 0xFF.toByte() && jpeg[1] == 0xD8.toByte())
  val tiff = byteArrayOf(
    'M'.code.toByte(), 'M'.code.toByte(), 0x00, 0x2A, // big-endian TIFF
    0x00, 0x00, 0x00, 0x08, // IFD0 offset
    0x00, 0x01, // one entry
    0x01, 0x12, 0x00, 0x03, 0x00, 0x00, 0x00, 0x01, // Orientation, SHORT, count 1
    (orientation shr 8).toByte(), orientation.toByte(), 0x00, 0x00, // value, padded
    0x00, 0x00, 0x00, 0x00, // no next IFD
  )
  val exif = "Exif\u0000\u0000".toByteArray(Charsets.US_ASCII) + tiff
  val len = exif.size + 2
  val app1 = byteArrayOf(0xFF.toByte(), 0xE1.toByte(), (len shr 8).toByte(), len.toByte()) + exif
  return jpeg.copyOfRange(0, 2) + app1 + jpeg.copyOfRange(2, jpeg.size)
}
