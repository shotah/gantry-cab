package com.gantree.cab.mailbox

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class MailboxClient(
  private val onFrame: (WireFrame) -> Unit,
  private val onState: (Boolean) -> Unit,
  private val onError: (Throwable, Response?) -> Unit = { _, _ -> },
  private val client: OkHttpClient = OkHttpClient.Builder()
    .pingInterval(20, TimeUnit.SECONDS)
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(0, TimeUnit.SECONDS)
    .build(),
) {
  private val socket = AtomicReference<WebSocket?>(null)
  private val stopped = AtomicBoolean(true)
  private val retrying = AtomicBoolean(false)
  private val attempt = AtomicInteger(0)
  @Volatile
  private var lastSeenId: String? = null
  private var lastSeenSeq: Int = 0
  private val seen = LinkedHashSet<String>()

  fun remember(id: String, seq: Int? = null) {
    synchronized(seen) { noteLocked(id, seq) }
  }

  fun start(origin: String, slug: String, bearer: String) {
    stopped.set(false)
    attempt.set(0)
    connect(origin, slug, bearer)
  }

  fun stop() {
    stopped.set(true)
    socket.getAndSet(null)?.cancel()
    onState(false)
  }

  fun send(frame: WireFrame): Boolean {
    val ws = socket.get() ?: return false
    return ws.send(encodeFrame(frame))
  }

  private fun noteLocked(id: String, seq: Int?) {
    val next = advanceCursor(ThreadCursor(id = lastSeenId, seq = lastSeenSeq), id, seq)
    lastSeenId = next.id
    lastSeenSeq = next.seq
    rememberSeen(seen, id)
  }

  private fun sinceLocked(): String? = ackSince(ThreadCursor(id = lastSeenId, seq = lastSeenSeq))

  private fun connect(origin: String, slug: String, bearer: String) {
    if (stopped.get()) {
      return
    }
    socket.getAndSet(null)?.cancel()
    val req = Request.Builder()
      .url(mailboxUrl(origin, slug))
      .header("Authorization", "Bearer $bearer")
      .build()
    val ws = client.newWebSocket(req, object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        if (stopped.get()) {
          webSocket.cancel()
          return
        }
        attempt.set(0)
        socket.set(webSocket)
        val since = synchronized(seen) { sinceLocked() }
        since?.let { webSocket.send(encodeFrame(ackSince(it))) }
        onState(true)
      }

      override fun onMessage(webSocket: WebSocket, text: String) {
        if (text == "ping") {
          webSocket.send("pong")
          return
        }
        val frame = parseFrame(text) ?: return
        val id = frame.id
        if (id != null && movesCursor(frame.kind)) {
          synchronized(seen) { noteLocked(id, frame.seq) }
        }
        onFrame(frame)
      }

      override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(1000, null)
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        onState(false)
        retry(origin, slug, bearer, null)
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        onError(t, response)
        onState(false)
        retry(origin, slug, bearer, response?.code)
      }
    })
    socket.set(ws)
  }

  private fun retry(origin: String, slug: String, bearer: String, httpCode: Int?) {
    if (stopped.get()) {
      return
    }
    if (!mailboxShouldRetry(httpCode)) {
      stopped.set(true)
      return
    }
    if (!retrying.compareAndSet(false, true)) {
      return
    }
    val delay = mailboxRetryDelayMs(attempt.getAndIncrement())
    Thread {
      try {
        Thread.sleep(delay)
      } catch (_: InterruptedException) {
        return@Thread
      } finally {
        retrying.set(false)
      }
      if (!stopped.get()) {
        connect(origin, slug, bearer)
      }
    }.start()
  }
}
