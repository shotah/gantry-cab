package com.gantree.cab.mailbox

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Read [uri] as a JPEG no larger than [maxBytes]. Draws at [edge] and walks
 * the pendant ladder ([shrinkSteps]) until one encode fits; only the floor
 * step failing throws `image too large`, same message as `jpegFromFile`.
 */
fun jpegFromUri(
  resolver: ContentResolver,
  uri: Uri,
  edge: Int = AVATAR_EDGE,
  maxBytes: Int = AVATAR_MAX_BYTES,
): ByteArray {
  val mime = resolver.getType(uri)?.lowercase().orEmpty().ifBlank { "image/jpeg" }
  val raw = resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("could not read that image")
  // Header only. With inJustDecodeBounds the decoder returns null by contract; read the out* fields.
  val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
  BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
  val width = bounds.outWidth
  val height = bounds.outHeight
  if (width <= 0 || height <= 0) {
    error("could not read that image")
  }
  if (shouldPassthroughJpeg(type = mime, size = raw.size, width = width, height = height, edge = edge, maxBytes = maxBytes)) {
    val check = acceptJpeg(raw)
    if (check is JpegCheck.Ok) {
      return raw
    }
  }
  val bmp = decodeUpright(raw, decodeSample(width, height, edge))
  val steps = shrinkSteps(edge, max(bmp.width, bmp.height))
  return shrinkToFit(steps, maxBytes) { step -> encodeJpeg(bmp, step) } ?: error("image too large")
}

/**
 * [ImageDecoder] applies EXIF orientation; [BitmapFactory] does not. A phone
 * camera stores the sensor frame landscape and tags it, so re-encoding through
 * BitmapFactory would hand the crane a portrait shot on its side. Software
 * allocation because the ladder scales and compresses the bitmap on the CPU.
 */
private fun decodeUpright(raw: ByteArray, sample: Int): Bitmap {
  val source = ImageDecoder.createSource(ByteBuffer.wrap(raw))
  return try {
    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
      decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
      decoder.setTargetSampleSize(sample)
    }
  } catch (_: IOException) {
    error("could not read that image")
  }
}

private fun encodeJpeg(bmp: Bitmap, step: JpegStep): ByteArray {
  val scale = minOf(1f, step.edge.toFloat() / max(bmp.width, bmp.height).toFloat())
  val dw = max(1, (bmp.width * scale).roundToInt())
  val dh = max(1, (bmp.height * scale).roundToInt())
  val scaled = if (dw == bmp.width && dh == bmp.height) bmp else Bitmap.createScaledBitmap(bmp, dw, dh, true)
  val out = ByteArrayOutputStream()
  try {
    if (!scaled.compress(Bitmap.CompressFormat.JPEG, step.quality, out)) {
      error("could not encode jpeg")
    }
  } finally {
    if (scaled !== bmp) {
      scaled.recycle()
    }
  }
  return out.toByteArray()
}

private fun decodeSample(width: Int, height: Int, edge: Int): Int {
  var sample = 1
  val longest = max(width, height)
  while (longest / sample > edge * 2) {
    sample *= 2
  }
  return sample
}

/**
 * Where the system camera writes an attach-menu capture. One fixed file in
 * app-private cache: `TakePicture` only reports success, so the callback
 * rebuilds this URI (even after a process restart) and the next shot
 * overwrites it. Authority `<applicationId>.fileprovider` is declared in
 * AndroidManifest with `res/xml/camera_paths.xml`.
 */
fun cameraShotUri(ctx: Context): Uri {
  val dir = File(ctx.cacheDir, "camera").apply { mkdirs() }
  return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", File(dir, "shot.jpg"))
}
