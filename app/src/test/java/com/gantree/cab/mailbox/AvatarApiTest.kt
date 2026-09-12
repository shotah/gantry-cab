package com.gantree.cab.mailbox

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlinx.coroutines.runBlocking
import java.io.File

class AvatarApiTest {
  @get:Rule
  val tmp = TemporaryFolder()
  private lateinit var server: MockWebServer
  private lateinit var api: AvatarApi

  @Before
  fun start() {
    server = MockWebServer()
    server.start()
    api = AvatarApi()
  }

  @After
  fun stop() {
    server.shutdown()
  }

  @Test
  fun fetchReturnsBytesAndSendsBearer() {
    val jpeg = fakeJpeg()
    server.enqueue(MockResponse().setBody(Buffer().write(jpeg)))
    val origin = server.url("/").toString()
    val got = runBlocking { api.fetch(origin, "kit", "jwe", 3) }
    assertTrue(got.contentEquals(jpeg))
    val req = server.takeRequest()
    assertEquals("/api/avatar?slug=kit&v=3", req.path)
    assertEquals("Bearer jwe", req.getHeader("Authorization"))
  }

  @Test
  fun fetchBackdropUsesTheBackdropPath() {
    val jpeg = fakeJpeg()
    server.enqueue(MockResponse().setBody(okio.Buffer().write(jpeg)))
    val got = runBlocking { api.fetch(server.url("/").toString(), "kit", "jwe", 4, "/api/backdrop") }
    assertTrue(got.contentEquals(jpeg))
    assertEquals("/api/backdrop?slug=kit&v=4", server.takeRequest().path)
  }

  @Test
  fun fetchMissIsNull() {
    server.enqueue(MockResponse().setResponseCode(404))
    assertNull(runBlocking { api.fetch(server.url("/").toString(), "kit", "", 0) })
    assertNull(server.takeRequest().getHeader("Authorization"))
  }

  @Test
  fun fetchKeepsTheLastFaceOnDiskAndCachedReadsItBack() {
    val cache = BlobCache(File(tmp.root, "blobs"))
    val api = AvatarApi(cache = cache)
    val origin = server.url("/").toString()
    assertNull(api.cached(origin, "kit"))
    val jpeg = fakeJpeg()
    server.enqueue(MockResponse().setBody(Buffer().write(jpeg)))
    runBlocking { api.fetch(origin, "kit", "jwe", 1) }
    assertTrue(jpeg.contentEquals(api.cached(origin, "kit")))
    assertTrue(jpeg.contentEquals(cache.read(blobCacheKey(origin, "kit", "/api/avatar"))))
    assertNull(api.cached(origin, "kit", "/api/backdrop"))
  }

  @Test
  fun fetchFailureAnswersWithTheCachedBytesNotBlank() {
    val cache = BlobCache(File(tmp.root, "blobs"))
    val api = AvatarApi(cache = cache)
    val origin = server.url("/").toString()
    val jpeg = fakeJpeg()
    server.enqueue(MockResponse().setBody(Buffer().write(jpeg)))
    runBlocking { api.fetch(origin, "kit", "jwe", 1) }
    server.enqueue(MockResponse().setResponseCode(503))
    assertTrue(jpeg.contentEquals(runBlocking { api.fetch(origin, "kit", "jwe", 1) }))
    server.enqueue(MockResponse().setResponseCode(401))
    assertTrue(jpeg.contentEquals(runBlocking { api.fetch(origin, "kit", "expired", 1) }))
    // Offline: nothing listens on port 1.
    val dead = "http://127.0.0.1:1"
    cache.write(blobCacheKey(dead, "kit", "/api/avatar"), jpeg)
    assertTrue(jpeg.contentEquals(runBlocking { api.fetch(dead, "kit", "jwe", 1) }))
  }

  @Test
  fun fetchMissForgetsTheCachedBytes() {
    val api = AvatarApi(cache = BlobCache(File(tmp.root, "blobs")))
    val origin = server.url("/").toString()
    server.enqueue(MockResponse().setBody(Buffer().write(fakeJpeg())))
    runBlocking { api.fetch(origin, "kit", "jwe", 1, "/api/backdrop") }
    assertTrue(api.cached(origin, "kit", "/api/backdrop") != null)
    server.enqueue(MockResponse().setResponseCode(404))
    assertNull(runBlocking { api.fetch(origin, "kit", "jwe", 0, "/api/backdrop") })
    assertNull(api.cached(origin, "kit", "/api/backdrop"))
  }

  @Test
  fun failureWithoutACacheIsStillNull() {
    server.enqueue(MockResponse().setResponseCode(503))
    assertNull(runBlocking { api.fetch(server.url("/").toString(), "kit", "jwe", 0) })
  }

  @Test
  fun uploadRejectsNonJpeg() {
    val got = api.upload(server.url("/").toString(), "kit", "jwe", byteArrayOf(1, 2, 3))
    assertEquals("image too small", (got as AvatarUpload.Err).error)
    assertEquals(0, server.requestCount)
  }

  @Test
  fun uploadPostsMultipartAndReturnsRev() {
    server.enqueue(MockResponse().setBody("""{"ok":true,"rev":9}"""))
    val got = api.upload(server.url("/").toString(), "kit", "jwe", fakeJpeg())
    assertEquals(9, (got as AvatarUpload.Ok).rev)
    val req = server.takeRequest()
    assertEquals("POST", req.method)
    assertTrue(req.getHeader("Content-Type")!!.startsWith("multipart/form-data"))
    assertEquals("Bearer jwe", req.getHeader("Authorization"))
  }

  @Test
  fun uploadSurfacesDetail() {
    server.enqueue(MockResponse().setResponseCode(400).setBody("""{"detail":"need a JPEG"}"""))
    val got = api.upload(server.url("/").toString(), "kit", "", fakeJpeg())
    assertEquals("need a JPEG", (got as AvatarUpload.Err).error)
  }

  @Test
  fun uploadNeedsAPositiveRev() {
    server.enqueue(MockResponse().setBody("""{"ok":true,"rev":0}"""))
    val got = api.upload(server.url("/").toString(), "kit", "", fakeJpeg())
    assertTrue(got is AvatarUpload.Err)
  }

  private fun fakeJpeg(n: Int = 128): ByteArray {
    val b = ByteArray(n)
    b[0] = 0xFF.toByte()
    b[1] = 0xD8.toByte()
    b[2] = 0xFF.toByte()
    b[n - 1] = 0xD9.toByte()
    return b
  }
}
