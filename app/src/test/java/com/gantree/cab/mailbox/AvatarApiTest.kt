package com.gantree.cab.mailbox

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.runBlocking

class AvatarApiTest {
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
  fun fetchMissIsNull() {
    server.enqueue(MockResponse().setResponseCode(404))
    assertNull(runBlocking { api.fetch(server.url("/").toString(), "kit", "", 0) })
    assertNull(server.takeRequest().getHeader("Authorization"))
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
