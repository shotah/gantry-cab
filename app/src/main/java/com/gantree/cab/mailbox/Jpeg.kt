package com.gantree.cab.mailbox

const val AVATAR_MAX_BYTES = 5 * 1024 * 1024
const val AVATAR_EDGE = 1280

sealed class JpegCheck {
  data object Ok : JpegCheck()
  data class Err(val detail: String) : JpegCheck()
  val ok: Boolean get() = this is Ok
}

fun acceptJpeg(bytes: ByteArray): JpegCheck {
  if (bytes.size < 32) {
    return JpegCheck.Err("image too small")
  }
  if (bytes.size > AVATAR_MAX_BYTES) {
    return JpegCheck.Err("image too large (max 5MB)")
  }
  if (bytes[0] != 0xFF.toByte() || bytes[1] != 0xD8.toByte() || bytes[2] != 0xFF.toByte()) {
    return JpegCheck.Err("need a JPEG (the console converts PNG/WebP on upload)")
  }
  return JpegCheck.Ok
}

fun shouldPassthroughJpeg(
  type: String,
  size: Int,
  width: Int,
  height: Int,
  edge: Int = AVATAR_EDGE,
  maxBytes: Int = AVATAR_MAX_BYTES,
): Boolean {
  val limit = edge.coerceAtLeast(1)
  val longest = maxOf(width, height).coerceAtLeast(1)
  val scale = minOf(1.0, limit.toDouble() / longest)
  return type == "image/jpeg" && scale == 1.0 && size <= maxBytes
}
