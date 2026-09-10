package com.gantree.cab.mailbox

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
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
  private var lastSeenId: String? = null
  private val seen = LinkedHashSet<String>()

  fun remember(id: String) {
    lastSeenId = id
    seen.add(id)
    while (seen.size > 200) {
      val first = seen.first()
      seen.remove(first)
    }
  }

  fun start(origin: String, slug: String, bearer: String) {
    stopped.set(false)
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
        onState(true)
        lastSeenId?.let { webSocket.send(encodeFrame(ackSince(it))) }
      }

      override fun onMessage(webSocket: WebSocket, text: String) {
        if (text == "ping") {
          webSocket.send("pong")
          return
        }
        val frame = parseFrame(text) ?: return
        val id = frame.id
        if (id != null) {
          if (!seen.add(id)) {
            return
          }
          lastSeenId = id
          while (seen.size > 200) {
            val first = seen.first()
            seen.remove(first)
          }
        }
        onFrame(frame)
      }

      override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(1000, null)
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        onState(false)
        retry(origin, slug, bearer)
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        onError(t, response)
        onState(false)
        retry(origin, slug, bearer)
      }
    })
    socket.set(ws)
  }

  private fun retry(origin: String, slug: String, bearer: String) {
    if (stopped.get()) {
      return
    }
    Thread {
      try {
        Thread.sleep(2_000)
      } catch (_: InterruptedException) {
        return@Thread
      }
      if (!stopped.get()) {
        connect(origin, slug, bearer)
      }
    }.start()
  }
}
