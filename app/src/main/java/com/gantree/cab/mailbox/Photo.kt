package com.gantree.cab.mailbox

import java.util.Base64

const val IMAGE_BYTES_MAX = 1_500_000
const val CHAT_PHOTO_EDGE = 1600
private const val IMAGE_B64_MAX = IMAGE_BYTES_MAX / 3 * 4 + 64

private val ALLOWED = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")

sealed class PhotoResult {
  data class Ok(val url: String) : PhotoResult()
  data class Err(val error: String) : PhotoResult()
  val ok: Boolean get() = this is Ok
}

fun photoDataUrl(bytes: ByteArray, mime: String = "image/jpeg"): PhotoResult {
  val kind = if (mime.lowercase() == "image/jpg") "image/jpeg" else mime.lowercase()
  if (kind !in ALLOWED) {
    return PhotoResult.Err("bad photo")
  }
  if (bytes.isEmpty()) {
    return PhotoResult.Err("bad photo")
  }
  if (bytes.size > IMAGE_BYTES_MAX) {
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
