package com.gantree.cab.mailbox

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthApiTest {
  private lateinit var server: MockWebServer
  private lateinit var api: AuthApi

  @Before
  fun start() {
    server = MockWebServer()
    server.start()
    api = AuthApi()
  }

  @After
  fun stop() {
    server.shutdown()
  }

  @Test
  fun configReadsModeAndGoogle() {
    server.enqueue(MockResponse().setBody("""{"mode":"google","google":true}"""))
    val got = api.config(server.url("/").toString())
    assertEquals("google", got.mode)
    assertEquals(true, got.google)
    assertEquals("/api/auth/config", server.takeRequest().path)
  }

  @Test
  fun meReadsCranesAndSkipsBlanks() {
    server.enqueue(MockResponse().setBody("""{"sub":"u1","email":"ada@example.com","cranes":["kit","", "dock","Nope!"]}"""))
    val got = api.me(server.url("/").toString(), "jwe")
    assertEquals("u1", got.sub)
    assertEquals("ada@example.com", got.email)
    assertEquals(listOf("kit", "dock"), got.cranes)
    assertEquals("Bearer jwe", server.takeRequest().getHeader("Authorization"))
  }

  @Test
  fun configDefaultsWhenTheBodyIsEmpty() {
    server.enqueue(MockResponse().setBody("{}"))
    val got = api.config(server.url("/").toString())
    assertEquals(null, got.mode)
    assertEquals(false, got.google)
  }

  @Test
  fun meWithoutCranesIsEmpty() {
    server.enqueue(MockResponse().setBody("""{"sub":"u1"}"""))
    val got = api.me(server.url("/").toString(), "jwe")
    assertEquals(null, got.email)
    assertEquals(emptyList<String>(), got.cranes)
  }

  @Test
  fun tokenPostsIdToken() {
    server.enqueue(MockResponse().setBody("""{"token":"jwe","sub":"u1","email":"ada@example.com","exp":9}"""))
    val got = api.token(server.url("/").toString(), "id", "nonce")
    assertEquals("jwe", got.token)
    assertEquals("u1", got.sub)
    assertEquals(9L, got.exp)
    val req = server.takeRequest()
    assertEquals("POST", req.method)
    assertTrue(req.body.readUtf8().contains("id_token"))
  }

  @Test
  fun failedStatusIsAuthException() {
    server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"nope"}"""))
    try {
      api.config(server.url("/").toString())
      throw AssertionError("expected AuthException")
    } catch (e: AuthException) {
      assertEquals(401, e.code)
      assertTrue(e.body.contains("nope"))
    }
  }
}
