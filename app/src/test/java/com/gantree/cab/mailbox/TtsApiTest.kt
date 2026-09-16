package com.gantree.cab.mailbox

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TtsApiTest {
  private lateinit var server: MockWebServer
  private lateinit var api: TtsApi

  @Before
  fun start() {
    server = MockWebServer()
    server.start()
    api = TtsApi()
  }

  @After
  fun stop() {
    server.shutdown()
  }

  @Test
  fun ttsUrlIsTheWorkerRoute() {
    assertEquals("https://pendant.example/api/tts", ttsUrl("https://pendant.example/"))
    assertEquals("http://10.0.2.2:3000/api/tts", ttsUrl(" http://10.0.2.2:3000 "))
  }

  @Test
  fun postsTheTextWithTheBearerAndHandsBackTheClip() {
    val mp3 = byteArrayOf(0x49, 0x44, 0x33, 0x04, 0x00)
    server.enqueue(
      MockResponse()
        .setHeader("Content-Type", "audio/mpeg")
        .setBody(Buffer().write(mp3)),
    )
    val got = api.synthesize(server.url("/").toString(), "jwe", "On my way.")
    assertTrue(got is TtsResult.Ok)
    assertArrayEquals(mp3, (got as TtsResult.Ok).mp3)
    val req = server.takeRequest()
    assertEquals("POST", req.method)
    assertEquals("/api/tts", req.path)
    assertEquals("Bearer jwe", req.getHeader("Authorization"))
    assertNull(req.getHeader("Cookie"))
    assertTrue(req.getHeader("Content-Type").orEmpty().startsWith("application/json"))
    val sent = JSONObject(req.body.readUtf8())
    assertEquals("On my way.", sent.getString("text"))
    assertEquals("en", sent.getString("lang"))
  }

  @Test
  fun languageRidesOnTheBodyAsTheSettingsId() {
    server.enqueue(MockResponse().setBody(Buffer().write(byteArrayOf(1))))
    api.synthesize(server.url("/").toString(), "jwe", "今夜は雨です。", lang = "ja")
    assertEquals("ja", JSONObject(server.takeRequest().body.readUtf8()).getString("lang"))
    // Never the recognizer tag, never junk: the Worker would drop it and speak the default anyway.
    server.enqueue(MockResponse().setBody(Buffer().write(byteArrayOf(1))))
    api.synthesize(server.url("/").toString(), "jwe", "hi", lang = "zh-CN")
    assertEquals("en", JSONObject(server.takeRequest().body.readUtf8()).getString("lang"))
  }

  @Test
  fun noBearerSendsNoAuthorizationHeader() {
    server.enqueue(MockResponse().setResponseCode(401))
    assertEquals(TtsResult.Err(SpeakFail.UNAUTHORIZED), api.synthesize(server.url("/").toString(), "", "hi"))
    assertNull(server.takeRequest().getHeader("Authorization"))
  }

  @Test
  fun workerStatusesNameTheReason() {
    val origin = server.url("/").toString()
    server.enqueue(MockResponse().setResponseCode(404))
    assertEquals(TtsResult.Err(SpeakFail.NO_VOICE), api.synthesize(origin, "jwe", "hi"))
    server.enqueue(MockResponse().setResponseCode(403).setBody("""{"error":"unauthorized"}"""))
    assertEquals(TtsResult.Err(SpeakFail.UNAUTHORIZED), api.synthesize(origin, "jwe", "hi"))
    server.enqueue(MockResponse().setResponseCode(429))
    assertEquals(TtsResult.Err(SpeakFail.BUSY), api.synthesize(origin, "jwe", "hi"))
    server.enqueue(MockResponse().setResponseCode(502).setBody("""{"error":"tts"}"""))
    assertEquals(TtsResult.Err(SpeakFail.VENDOR), api.synthesize(origin, "jwe", "hi"))
  }

  @Test
  fun anEmptyClipIsAVendorFailure() {
    server.enqueue(MockResponse().setResponseCode(200))
    assertEquals(TtsResult.Err(SpeakFail.VENDOR), api.synthesize(server.url("/").toString(), "jwe", "hi"))
  }

  @Test
  fun anUnreachableWorkerIsOffline() {
    val origin = server.url("/").toString()
    server.shutdown()
    assertEquals(TtsResult.Err(SpeakFail.OFFLINE), api.synthesize(origin, "jwe", "hi"))
  }
}
