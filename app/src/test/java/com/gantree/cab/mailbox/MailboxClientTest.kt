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
  fun openAcksLastSeenAndDeliversFrames() {
    val frames = CopyOnWriteArrayList<WireFrame>()
    val acks = CopyOnWriteArrayList<String>()
    val up = CountDownLatch(1)
    val ack = CountDownLatch(1)
    val gotThree = CountDownLatch(1)
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
        if (frames.size >= 3) gotThree.countDown()
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
      assertTrue(gotThree.await(5, TimeUnit.SECONDS))
      assertEquals(listOf("hi", "again", "two"), frames.map { it.text })
      assertTrue(client.send(inbound("yo", "c", null)))
    } finally {
      client.stop()
    }
  }

  @Test
  fun onStateFiresAfterAckSinceSoAnOutboxSendIsSecond() {
    val got = CopyOnWriteArrayList<String>()
    val two = CountDownLatch(1)
    val up = CountDownLatch(1)
    lateinit var client: MailboxClient
    server.enqueue(
      MockResponse().withWebSocketUpgrade(
        object : WebSocketListener() {
          override fun onMessage(webSocket: WebSocket, text: String) {
            got.add(text)
            if (got.size >= 2) two.countDown()
          }
        },
      ),
    )
    client = MailboxClient(
      onFrame = {},
      onState = {
        if (it) {
          client.send(inbound("queued", "q1", null))
          up.countDown()
        }
      },
    )
    client.remember("old")
    client.start(server.url("/").toString(), "kit", "tok")
    try {
      assertTrue(up.await(5, TimeUnit.SECONDS))
      assertTrue(two.await(5, TimeUnit.SECONDS))
      assertTrue(got[0].contains("\"since\":\"old\""))
      assertTrue(got[1].contains("queued"))
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

  @Test
  fun aRefusalDoesNotMoveTheSinceCursor() {
    val sinces = CopyOnWriteArrayList<String>()
    val reconnectAck = CountDownLatch(1)
    val opened = CountDownLatch(1)
    val peer = AtomicReference<WebSocket>()
    val listener = object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        if (peer.compareAndSet(null, webSocket)) opened.countDown()
      }

      override fun onMessage(webSocket: WebSocket, text: String) {
        if (text.contains("\"since\"")) {
          sinces.add(text)
          reconnectAck.countDown()
        }
      }
    }
    server.enqueue(MockResponse().withWebSocketUpgrade(listener))
    server.enqueue(MockResponse().withWebSocketUpgrade(listener))
    val gotTwo = CountDownLatch(2)
    val client = MailboxClient(onFrame = { gotTwo.countDown() }, onState = {})
    client.start(server.url("/").toString(), "kit", "tok")
    try {
      assertTrue(opened.await(5, TimeUnit.SECONDS))
      peer.get().send("""{"kind":"reply","id":"a","text":"hi"}""")
      // The mailbox names the frame it refused; that id was never queued, so it is not a resume point.
      peer.get().send("""{"kind":"error","text":"rate","id":"refused"}""")
      assertTrue(gotTwo.await(5, TimeUnit.SECONDS))
      peer.get().close(1000, "bye")
      assertTrue(reconnectAck.await(10, TimeUnit.SECONDS))
      assertTrue(sinces.single().contains("\"since\":\"a\""))
    } finally {
      client.stop()
    }
  }

  @Test
  fun sweepSwapsInASecondSocketWithoutADownState() {
    val states = CopyOnWriteArrayList<Boolean>()
    val frames = CopyOnWriteArrayList<WireFrame>()
    val peers = CopyOnWriteArrayList<WebSocket>()
    val heard = CopyOnWriteArrayList<Pair<WebSocket, String>>()
    val firstOpen = CountDownLatch(1)
    val secondAck = CountDownLatch(1)
    val firstGone = CountDownLatch(1)
    val secondHeardSend = CountDownLatch(1)
    val listener = object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        peers.add(webSocket)
        firstOpen.countDown()
      }

      override fun onMessage(webSocket: WebSocket, text: String) {
        heard.add(webSocket to text)
        if (peers.size >= 2 && webSocket === peers[1]) {
          if (text.contains("\"since\"")) secondAck.countDown()
          if (text.contains("after")) secondHeardSend.countDown()
        }
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        if (webSocket === peers.firstOrNull()) firstGone.countDown()
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        if (webSocket === peers.firstOrNull()) firstGone.countDown()
      }
    }
    server.enqueue(MockResponse().withWebSocketUpgrade(listener))
    server.enqueue(MockResponse().withWebSocketUpgrade(listener))
    val gotOne = CountDownLatch(1)
    val gotTwo = CountDownLatch(2)
    val client = MailboxClient(
      onFrame = {
        frames.add(it)
        gotOne.countDown()
        gotTwo.countDown()
      },
      onState = { states.add(it) },
    )
    assertFalse(client.sweep(Long.MAX_VALUE))
    client.start(server.url("/").toString(), "kit", "tok")
    try {
      assertTrue(firstOpen.await(5, TimeUnit.SECONDS))
      peers[0].send("""{"kind":"reply","id":"a","text":"hi","seq":3}""")
      assertTrue(gotOne.await(5, TimeUnit.SECONDS))
      // Just opened: not worth a handshake yet.
      assertFalse(client.sweep())
      assertTrue(client.sweep(System.currentTimeMillis() + SWEEP_MIN_GAP_MS))
      assertTrue(secondAck.await(5, TimeUnit.SECONDS))
      assertTrue(heard.last { it.first === peers[1] }.second.contains("\"since\":\"3\""))
      assertTrue(firstGone.await(5, TimeUnit.SECONDS))
      // The mailbox flushes what the phone missed onto the new socket; it folds in like any frame.
      peers[1].send("""{"kind":"inbound","id":"b","text":"from the browser","seq":4,"replay":true}""")
      assertTrue(gotTwo.await(5, TimeUnit.SECONDS))
      assertEquals(listOf("hi", "from the browser"), frames.map { it.text })
      assertTrue(client.send(inbound("after", "c", null)))
      assertTrue(secondHeardSend.await(5, TimeUnit.SECONDS))
      assertEquals(listOf(true, true), states)
    } finally {
      client.stop()
    }
  }

  @Test
  fun aSweepThatCannotOpenIsSilentAndKeepsTheFirstSocket() {
    val states = CopyOnWriteArrayList<Boolean>()
    val errors = CopyOnWriteArrayList<Int>()
    val peer = AtomicReference<WebSocket>()
    val opened = CountDownLatch(1)
    val heardSend = CountDownLatch(1)
    server.enqueue(
      MockResponse().withWebSocketUpgrade(
        object : WebSocketListener() {
          override fun onOpen(webSocket: WebSocket, response: Response) {
            peer.set(webSocket)
            opened.countDown()
          }

          override fun onMessage(webSocket: WebSocket, text: String) {
            if (text.contains("still")) heardSend.countDown()
          }
        },
      ),
    )
    server.enqueue(MockResponse().setResponseCode(503))
    val gotOne = CountDownLatch(1)
    val up = CountDownLatch(1)
    val client = MailboxClient(
      onFrame = { gotOne.countDown() },
      onState = {
        states.add(it)
        if (it) up.countDown()
      },
      onError = { _, res -> errors.add(res?.code ?: -1) },
    )
    client.start(server.url("/").toString(), "kit", "tok")
    try {
      assertTrue(opened.await(5, TimeUnit.SECONDS))
      assertTrue(up.await(5, TimeUnit.SECONDS))
      assertTrue(client.sweep(System.currentTimeMillis() + SWEEP_MIN_GAP_MS))
      // Both dials reached the server; let the refused one fail.
      server.takeRequest(5, TimeUnit.SECONDS)
      server.takeRequest(5, TimeUnit.SECONDS)
      Thread.sleep(300)
      peer.get().send("""{"kind":"reply","id":"a","text":"hi"}""")
      assertTrue(gotOne.await(5, TimeUnit.SECONDS))
      assertTrue(client.send(inbound("still here", "c", null)))
      assertTrue(heardSend.await(5, TimeUnit.SECONDS))
      assertEquals(listOf(true), states)
      assertTrue(errors.isEmpty())
    } finally {
      client.stop()
      assertFalse(client.sweep(Long.MAX_VALUE))
    }
  }

  @Test
  fun openAcksHighestSeqNotLastArrival() {
    val acks = CopyOnWriteArrayList<String>()
    val ack = CountDownLatch(1)
    val up = CountDownLatch(1)
    server.enqueue(
      MockResponse().withWebSocketUpgrade(
        object : WebSocketListener() {
          override fun onMessage(webSocket: WebSocket, text: String) {
            acks.add(text)
            ack.countDown()
          }
        },
      ),
    )
    val client = MailboxClient(
      onFrame = {},
      onState = { if (it) up.countDown() },
    )
    client.remember("b", 2)
    client.remember("a", 1)
    client.start(server.url("/").toString(), "kit", "tok")
    try {
      assertTrue(up.await(5, TimeUnit.SECONDS))
      assertTrue(ack.await(5, TimeUnit.SECONDS))
      assertTrue(acks.any { it.contains("\"since\":\"2\"") })
    } finally {
      client.stop()
    }
  }
}
