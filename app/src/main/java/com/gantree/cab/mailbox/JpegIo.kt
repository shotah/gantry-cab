package com.gantree.cab.mailbox

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
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
  val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
  resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    ?: error("could not read that image")
  val width = bounds.outWidth
  val height = bounds.outHeight
  if (width <= 0 || height <= 0) {
    error("could not read that image")
  }
  val raw = resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("could not read that image")
  if (shouldPassthroughJpeg(type = mime, size = raw.size, width = width, height = height, edge = edge, maxBytes = maxBytes)) {
    val check = acceptJpeg(raw)
    if (check is JpegCheck.Ok) {
      return raw
    }
  }
  val sample = decodeSample(width, height, edge)
  val opts = BitmapFactory.Options().apply { inSampleSize = sample }
  val bmp = BitmapFactory.decodeByteArray(raw, 0, raw.size, opts) ?: error("could not read that image")
  val steps = shrinkSteps(edge, max(bmp.width, bmp.height))
  return shrinkToFit(steps, maxBytes) { step -> encodeJpeg(bmp, step) } ?: error("image too large")
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
