package com.gantree.cab.mailbox

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/** What the last 200 left on disk: bytes plus the rev that names them (0 = unknown). */
class CachedBlob(val rev: Int, val bytes: ByteArray)

/**
 * Last JPEG per room on disk, so the face and wallpaper paint on launch
 * before the mailbox answers — pendant `look:<kind>:<slug>` in IndexedDB.
 * Every launch still asks the mailbox, with `If-None-Match: "<rev>"` so an
 * unchanged blob is an empty 304 instead of the JPEG again. Pendant serves
 * `max-age=0, must-revalidate`, which keeps OkHttp's own cache from ever
 * painting first; this one does.
 */
class BlobCache(private val dir: File) {
  fun read(key: String): CachedBlob? {
    return try {
      val bytes = File(dir, key).takeIf { it.isFile }?.readBytes()?.takeIf { it.isNotEmpty() } ?: return null
      CachedBlob(rev(key), bytes)
    } catch (_: Exception) {
      null
    }
  }

  /** Rev of the bytes on disk without reading them; 0 when none or unknown. */
  fun rev(key: String): Int {
    return try {
      if (!File(dir, key).isFile) {
        return 0
      }
      File(dir, "$key.rev").takeIf { it.isFile }?.readText()?.trim()?.toIntOrNull()?.takeIf { it > 0 } ?: 0
    } catch (_: Exception) {
      0
    }
  }

  /** Null or empty forgets the room's blob (the mailbox said 404). */
  fun write(key: String, blob: CachedBlob?) {
    try {
      val file = File(dir, key)
      val revFile = File(dir, "$key.rev")
      // Rev goes last: stale bytes with a fresh rev would 304 forever.
      revFile.delete()
      if (blob == null || blob.bytes.isEmpty()) {
        file.delete()
        return
      }
      dir.mkdirs()
      val tmp = File(dir, "$key.tmp")
      tmp.writeBytes(blob.bytes)
      Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
      if (blob.rev > 0) {
        revFile.writeText(blob.rev.toString())
      }
    } catch (_: Exception) {
      /* a cache that cannot write is still a cache */
    }
  }
}

/** Stable file name per blob path + origin + room: `api-avatar-<16 hex>`. No rev or bearer in it. */
fun blobCacheKey(origin: String, slug: String, path: String): String {
  val kind = path.trim('/').replace('/', '-')
  val digest = MessageDigest.getInstance("SHA-256")
    .digest("${httpOrigin(origin)}|$slug".toByteArray(Charsets.UTF_8))
  val hex = digest.take(8).joinToString("") { "%02x".format(it) }
  return "$kind-$hex"
}
