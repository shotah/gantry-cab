package com.gantree.cab.mailbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BlobCacheTest {
  @get:Rule
  val tmp = TemporaryFolder()

  @Test
  fun writeThenReadRoundTripsAndOverwrites() {
    val cache = BlobCache(File(tmp.root, "blobs"))
    val key = blobCacheKey("https://m.example", "kit", "/api/avatar")
    assertNull(cache.read(key))
    cache.write(key, byteArrayOf(1, 2, 3))
    assertTrue(byteArrayOf(1, 2, 3).contentEquals(cache.read(key)))
    cache.write(key, byteArrayOf(9))
    assertTrue(byteArrayOf(9).contentEquals(cache.read(key)))
    assertFalse(File(tmp.root, "blobs/$key.tmp").exists())
  }

  @Test
  fun nullOrEmptyForgetsTheBlob() {
    val cache = BlobCache(tmp.root)
    cache.write("k", byteArrayOf(1))
    cache.write("k", null)
    assertNull(cache.read("k"))
    cache.write("k", byteArrayOf(1))
    cache.write("k", ByteArray(0))
    assertNull(cache.read("k"))
  }

  @Test
  fun unwritableDirIsQuietAndReadsNull() {
    val blocked = File(tmp.root, "file-not-dir")
    blocked.writeText("x")
    val cache = BlobCache(blocked)
    cache.write("k", byteArrayOf(1))
    assertNull(cache.read("k"))
  }

  @Test
  fun keyIsStableAndSplitsByPathRoomAndOrigin() {
    val a = blobCacheKey("https://m.example/", "kit", "/api/avatar")
    assertEquals(a, blobCacheKey("https://m.example", "kit", "/api/avatar"))
    assertTrue(a.startsWith("api-avatar-"))
    assertTrue(Regex("^api-avatar-[0-9a-f]{16}$").matches(a))
    assertTrue(blobCacheKey("https://m.example", "kit", "/api/backdrop").startsWith("api-backdrop-"))
    assertNotEquals(a, blobCacheKey("https://m.example", "ada", "/api/avatar"))
    assertNotEquals(a, blobCacheKey("https://other.example", "kit", "/api/avatar"))
  }
}
