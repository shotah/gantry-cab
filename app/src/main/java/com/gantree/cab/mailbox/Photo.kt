package com.gantree.cab.mailbox

import java.util.Base64
import kotlin.math.roundToInt

/** Pendant caps `utf8Bytes(url)` of the whole `data:` URL, not the JPEG inside it. */
const val IMAGE_BYTES_MAX = 1_500_000
const val CHAT_PHOTO_EDGE = 1600
private const val IMAGE_B64_MAX = IMAGE_BYTES_MAX / 3 * 4 + 64

/**
 * Raw JPEG budget. Base64 is 4/3 of the bytes plus the `data:image/jpeg;base64,`
 * prefix, so encode to this, not [IMAGE_BYTES_MAX]. Mirrors pendant `lib/phone/photo.ts`.
 */
const val PHOTO_JPEG_BYTES_MAX = (IMAGE_BYTES_MAX - 32) / 4 * 3

/** Quality ladder per edge, then the edge shrinks by [JPEG_EDGE_STEP] until [JPEG_EDGE_MIN]. */
val JPEG_QUALITY_STEPS = listOf(90, 80, 70, 60)
const val JPEG_EDGE_STEP = 0.75
const val JPEG_EDGE_MIN = 320

/**
 * Settings → Photo size. Long edge in px. Vision models bill by pixel area
 * (~w·h/750 tokens) and clamp near 1 MP, so Full mostly buys wire bytes, not
 * detail. Same ids and edges as the pendant PWA so "Medium" means one thing.
 */
data class PhotoSize(val id: String, val label: String, val edge: Int)

val PHOTO_SIZES = listOf(
  PhotoSize("full", "Full", CHAT_PHOTO_EDGE),
  PhotoSize("medium", "Medium", 1024),
  PhotoSize("small", "Small", 640),
)
val PHOTO_SIZE_IDS = PHOTO_SIZES.map { it.id }
const val DEFAULT_PHOTO_SIZE = "medium"

private val ALLOWED = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")

/** One JPEG encode on the ladder: longest edge in px and `Bitmap.compress` quality. */
data class JpegStep(val edge: Int, val quality: Int)

sealed class PhotoResult {
  data class Ok(val url: String) : PhotoResult()
  data class Err(val error: String) : PhotoResult()
  val ok: Boolean get() = this is Ok
}

fun parsePhotoSize(v: String?): String =
  if (v != null && v in PHOTO_SIZE_IDS) v else DEFAULT_PHOTO_SIZE

private fun photoSize(id: String?): PhotoSize {
  val want = parsePhotoSize(id)
  return PHOTO_SIZES.first { it.id == want }
}

fun photoEdge(id: String?): Int = photoSize(id).edge

fun photoSizeLabel(id: String?): String = photoSize(id).label

/** Settings chip text, same as the PWA `<option>`: "Medium · 1024 px". */
fun photoSizeChip(id: String?): String = photoSize(id).let { "${it.label} · ${it.edge} px" }

/**
 * Encode attempts, biggest first. Draw at `min(edge, longest)` (never upscale),
 * walk [JPEG_QUALITY_STEPS], then edge × [JPEG_EDGE_STEP] and repeat until the
 * next edge would drop under [JPEG_EDGE_MIN]. Mirrors pendant `jpegFromFile`.
 */
fun shrinkSteps(edge: Int, longest: Int): List<JpegStep> = buildList {
  var target = minOf(edge, longest).coerceAtLeast(1)
  while (true) {
    for (q in JPEG_QUALITY_STEPS) {
      add(JpegStep(target, q))
    }
    val next = (target * JPEG_EDGE_STEP).roundToInt()
    if (next < JPEG_EDGE_MIN) {
      break
    }
    target = next
  }
}

/** First [steps] encode that fits [maxBytes], or null when even the smallest is over. */
fun shrinkToFit(steps: List<JpegStep>, maxBytes: Int, encode: (JpegStep) -> ByteArray): ByteArray? {
  for (step in steps) {
    val bytes = encode(step)
    if (bytes.size <= maxBytes) {
      return bytes
    }
  }
  return null
}

fun photoDataUrl(bytes: ByteArray, mime: String = "image/jpeg"): PhotoResult {
  val kind = if (mime.lowercase() == "image/jpg") "image/jpeg" else mime.lowercase()
  if (kind !in ALLOWED) {
    return PhotoResult.Err("bad photo")
  }
  if (bytes.isEmpty()) {
    return PhotoResult.Err("bad photo")
  }
  if (bytes.size > PHOTO_JPEG_BYTES_MAX) {
    return PhotoResult.Err("too large")
  }
  val b64 = Base64.getEncoder().encodeToString(bytes)
  return PhotoResult.Ok("data:$kind;base64,$b64")
}

fun parsePhotoFile(type: String, size: Int): PhotoResult {
  val kind = type.lowercase()
  if (kind !in ALLOWED) {
    return PhotoResult.Err("bad photo")
  }
  if (size <= 0) {
    return PhotoResult.Err("bad photo")
  }
  if (size > IMAGE_BYTES_MAX) {
    return PhotoResult.Err("too large")
  }
  return PhotoResult.Ok("")
}

fun decodeDataUrl(url: String): ByteArray? {
  val marker = "base64,"
  val i = url.indexOf(marker)
  if (i < 0 || !url.startsWith("data:image/")) {
    return null
  }
  return try {
    val b64 = url.substring(i + marker.length).replace("\n", "")
    if (b64.length > IMAGE_B64_MAX) {
      return null
    }
    val bytes = Base64.getDecoder().decode(b64)
    bytes.takeIf { it.size <= IMAGE_BYTES_MAX }
  } catch (_: Exception) {
    null
  }
}
