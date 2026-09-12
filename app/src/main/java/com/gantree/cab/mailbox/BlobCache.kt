package com.gantree.cab.mailbox

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/**
 * Last JPEG per room on disk, so the face and wallpaper paint on launch
 * before the mailbox answers. Not a freshness cache: every launch still
 * asks `/api/avatar` / `/api/backdrop`, and a `face` / `backdrop` notice
 * refetches. Pendant serves these `max-age=0, must-revalidate`, which
 * keeps OkHttp's own cache from ever painting first.
 */
class BlobCache(private val dir: File) {
  fun read(key: String): ByteArray? {
    return try {
      File(dir, key).takeIf { it.isFile }?.readBytes()?.takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
      null
    }
  }

  /** Null or empty forgets the room's blob (the mailbox said 404). */
  fun write(key: String, bytes: ByteArray?) {
    try {
      val file = File(dir, key)
      if (bytes == null || bytes.isEmpty()) {
        file.delete()
        return
      }
      dir.mkdirs()
      val tmp = File(dir, "$key.tmp")
      tmp.writeBytes(bytes)
      Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    } catch (_: Exception) {
      /* a cache that cannot write is still a cache */
    }
  }
}

/** Stable file name per blob path + origin + room: `api-avatar-<16 hex>`. */
fun blobCacheKey(origin: String, slug: String, path: String): String {
  val kind = path.trim('/').replace('/', '-')
  val digest = MessageDigest.getInstance("SHA-256")
    .digest("${httpOrigin(origin)}|$slug".toByteArray(Charsets.UTF_8))
  val hex = digest.take(8).joinToString("") { "%02x".format(it) }
  return "$kind-$hex"
}
