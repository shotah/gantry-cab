package com.gantree.cab.mailbox

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class MailboxClientTest {
  private lateinit var server: MockWebServer

  @Before
  fun start() {
    server = MockWebServer()
    server.start()
  }

  @After
  fun stop() {
    server.shutdown()
  }

  @Test
  fun sendWithoutASocketFails() {
    val client = MailboxClient(onFrame = {}, onState = {})
    assertFalse(client.send(inbound("hi", "1", null)))
    client.stop()
  }

  @Test
  fun openAcksLastSeenAndDropsDuplicateIds() {
    val frames = CopyOnWriteArrayList<WireFrame>()
    val acks = CopyOnWriteArrayList<String>()
    val up = CountDownLatch(1)
    val ack = CountDownLatch(1)
    val gotTwo = CountDownLatch(1)
    val peer = AtomicReference<WebSocket>()
    server.enqueue(
      MockResponse().withWebSocketUpgrade(
        object : WebSocketListener() {
          override fun onOpen(webSocket: WebSocket, response: Response) {
            peer.set(webSocket)
          }

          override fun onMessage(webSocket: WebSocket, text: String) {
            acks.add(text)
            ack.countDown()
          }
        },
      ),
    )
    val client = MailboxClient(
      onFrame = {
        frames.add(it)
        if (frames.size >= 2) gotTwo.countDown()
      },
      onState = { if (it) up.countDown() },
    )
    client.remember("old")
    client.start(server.url("/").toString(), "kit", "tok")
    try {
      assertTrue(up.await(5, TimeUnit.SECONDS))
      assertTrue(ack.await(5, TimeUnit.SECONDS))
      assertTrue(acks.any { it.contains("\"since\":\"old\"") })
      val ws = peer.get()
      ws.send("""{"kind":"reply","id":"a","text":"hi"}""")
      ws.send("""{"kind":"reply","id":"a","text":"again"}""")
      ws.send("""{"kind":"reply","id":"b","text":"two"}""")
      assertTrue(gotTwo.await(5, TimeUnit.SECONDS))
      assertEquals(listOf("hi", "two"), frames.map { it.text })
      assertTrue(client.send(inbound("yo", "c", null)))
    } finally {
      client.stop()
    }
  }

  @Test
  fun pingGetsPongAndJunkIsDropped() {
    val pong = CountDownLatch(1)
    val frames = CopyOnWriteArrayList<WireFrame>()
    val peer = AtomicReference<WebSocket>()
    val opened = CountDownLatch(1)
    server.enqueue(
      MockResponse().withWebSocketUpgrade(
        object : WebSocketListener() {
          override fun onOpen(webSocket: WebSocket, response: Response) {
            peer.set(webSocket)
            opened.countDown()
          }

          override fun onMessage(webSocket: WebSocket, text: String) {
            if (text == "pong") pong.countDown()
          }
        },
      ),
    )
    val gotCmds = CountDownLatch(1)
    val client = MailboxClient(
      onFrame = {
        frames.add(it)
        gotCmds.countDown()
      },
      onState = {},
    )
    client.start(server.url("/").toString(), "kit", "tok")
    try {
      assertTrue(opened.await(5, TimeUnit.SECONDS))
      peer.get().send("ping")
      peer.get().send("nope")
      peer.get().send("""{"kind":"cmds","commands":[{"name":"new","hint":"reset this session"}]}""")
      assertTrue(pong.await(5, TimeUnit.SECONDS))
      assertTrue(gotCmds.await(5, TimeUnit.SECONDS))
      assertEquals("cmds", frames.single().kind)
    } finally {
      client.stop()
    }
  }

  @Test
  fun httpRefusalSurfacesOnError() {
    val err = CountDownLatch(1)
    val code = AtomicInteger(0)
    server.enqueue(MockResponse().setResponseCode(403))
    val client = MailboxClient(
      onFrame = {},
      onState = {},
      onError = { _, res ->
        code.set(res?.code ?: -1)
        err.countDown()
      },
    )
    client.start(server.url("/").toString(), "kit", "tok")
    try {
      assertTrue(err.await(5, TimeUnit.SECONDS))
      assertEquals(403, code.get())
    } finally {
      client.stop()
    }
  }
}
