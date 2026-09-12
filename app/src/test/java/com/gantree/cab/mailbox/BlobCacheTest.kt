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
  fun writeThenReadRoundTripsBytesAndRevAndOverwrites() {
    val cache = BlobCache(File(tmp.root, "blobs"))
    val key = blobCacheKey("https://m.example", "kit", "/api/avatar")
    assertNull(cache.read(key))
    assertEquals(0, cache.rev(key))
    cache.write(key, CachedBlob(3, byteArrayOf(1, 2, 3)))
    assertTrue(byteArrayOf(1, 2, 3).contentEquals(cache.read(key)!!.bytes))
    assertEquals(3, cache.read(key)!!.rev)
    assertEquals(3, cache.rev(key))
    cache.write(key, CachedBlob(4, byteArrayOf(9)))
    assertTrue(byteArrayOf(9).contentEquals(cache.read(key)!!.bytes))
    assertEquals(4, cache.rev(key))
    assertFalse(File(tmp.root, "blobs/$key.tmp").exists())
  }

  @Test
  fun unknownRevIsZeroAndNeverSentAsAnEtag() {
    val cache = BlobCache(tmp.root)
    cache.write("k", CachedBlob(5, byteArrayOf(1)))
    cache.write("k", CachedBlob(0, byteArrayOf(2)))
    assertEquals(0, cache.rev("k"))
    assertEquals(0, cache.read("k")!!.rev)
    assertTrue(byteArrayOf(2).contentEquals(cache.read("k")!!.bytes))
  }

  @Test
  fun nullOrEmptyForgetsTheBlobAndItsRev() {
    val cache = BlobCache(tmp.root)
    cache.write("k", CachedBlob(2, byteArrayOf(1)))
    cache.write("k", null)
    assertNull(cache.read("k"))
    assertEquals(0, cache.rev("k"))
    assertFalse(File(tmp.root, "k.rev").exists())
    cache.write("k", CachedBlob(2, byteArrayOf(1)))
    cache.write("k", CachedBlob(3, ByteArray(0)))
    assertNull(cache.read("k"))
  }

  @Test
  fun aRevWithoutBytesIsZero() {
    val cache = BlobCache(tmp.root)
    File(tmp.root, "k.rev").writeText("9")
    assertEquals(0, cache.rev("k"))
    assertNull(cache.read("k"))
  }

  @Test
  fun unwritableDirIsQuietAndReadsNull() {
    val blocked = File(tmp.root, "file-not-dir")
    blocked.writeText("x")
    val cache = BlobCache(blocked)
    cache.write("k", CachedBlob(1, byteArrayOf(1)))
    assertNull(cache.read("k"))
    assertEquals(0, cache.rev("k"))
  }

  @Test
  fun keyIsStableAndSplitsByPathRoomAndOrigin() {
    val a = blobCacheKey("https://m.example/", "kit", "/api/avatar")
    assertEquals(a, blobCacheKey("https://m.example", "kit", "/api/avatar"))
    assertTrue(Regex("^api-avatar-[0-9a-f]{16}$").matches(a))
    assertTrue(blobCacheKey("https://m.example", "kit", "/api/backdrop").startsWith("api-backdrop-"))
    assertNotEquals(a, blobCacheKey("https://m.example", "ada", "/api/avatar"))
    assertNotEquals(a, blobCacheKey("https://other.example", "kit", "/api/avatar"))
  }
}
