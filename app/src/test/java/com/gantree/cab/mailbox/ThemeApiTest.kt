package com.gantree.cab.mailbox

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ThemeApiTest {
  private lateinit var server: MockWebServer
  private lateinit var api: ThemeApi

  @Before
  fun start() {
    server = MockWebServer()
    server.start()
    api = ThemeApi()
  }

  @After
  fun stop() {
    server.shutdown()
  }

  @Test
  fun roomThemeFromStateReadsTheIdAndTreatsNullAsCleared() {
    assertEquals("tide", roomThemeFromState("""{"theme":"tide","themes":[]}"""))
    assertEquals("", roomThemeFromState("""{"theme":null}"""))
    assertEquals("", roomThemeFromState("""{"theme":"nope"}"""))
    assertEquals("", roomThemeFromState("nope"))
  }

  @Test
  fun fetchReturnsTheRoomIdAndSendsBearer() {
    server.enqueue(MockResponse().setBody("""{"theme":"noir","themes":[]}"""))
    val origin = server.url("/").toString()
    assertEquals("noir", api.fetch(origin, "kit", "jwe"))
    val req = server.takeRequest()
    assertEquals("/api/theme?slug=kit", req.path)
    assertEquals("Bearer jwe", req.getHeader("Authorization"))
  }

  @Test
  fun fetchMissOrJunkIsClearedNotANetworkFail() {
    server.enqueue(MockResponse().setBody("""{"theme":null}"""))
    assertEquals("", api.fetch(server.url("/").toString(), "kit", ""))
    assertNull(server.takeRequest().getHeader("Authorization"))
  }

  @Test
  fun fetchHttpErrorIsNullSoALiveNoticeIsKept() {
    server.enqueue(MockResponse().setResponseCode(503))
    assertNull(api.fetch(server.url("/").toString(), "kit", "jwe"))
  }
}
