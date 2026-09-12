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
  /** One dial. Callbacks from a line that is no longer [line] are noise. */
  private class Line

  private val line = AtomicReference<Line?>(null)
  private val socket = AtomicReference<WebSocket?>(null)
  private val stopped = AtomicBoolean(true)
  private val retrying = AtomicBoolean(false)
  private val attempt = AtomicInteger(0)
  private val up = AtomicBoolean(false)
  @Volatile
  private var openedAt = 0L
  @Volatile
  private var room: Triple<String, String, String>? = null
  @Volatile
  private var lastSeenId: String? = null
  private var lastSeenSeq: Int = 0
  private val seen = LinkedHashSet<String>()

  fun remember(id: String, seq: Int? = null) {
    synchronized(seen) { noteLocked(id, seq) }
  }

  fun start(origin: String, slug: String, bearer: String) {
    room = Triple(origin, slug, bearer)
    stopped.set(false)
    attempt.set(0)
    connect(origin, slug, bearer)
  }

  fun stop() {
    stopped.set(true)
    up.set(false)
    line.set(null)
    socket.getAndSet(null)?.cancel()
    onState(false)
  }

  fun send(frame: WireFrame): Boolean {
    val ws = socket.get() ?: return false
    return ws.send(encodeFrame(frame))
  }

  /**
   * Quiet catch-up. The mailbox has no "what did I miss" request, but a
   * connect is one: it flushes the transcript and the unread queue. Dial a
   * second socket and, only once it is open, swap it in and retire the
   * first — `onState` never sees a gap and nothing on screen is cleared.
   * Frames already held restamp by id; missing ones slot in by seq. A sweep
   * that cannot open is silent; the first socket stays. Not worth a
   * handshake within [SWEEP_MIN_GAP_MS] of the last open.
   */
  fun sweep(now: Long = System.currentTimeMillis()): Boolean {
    val (origin, slug, bearer) = room ?: return false
    val current = line.get() ?: return false
    if (stopped.get() || !up.get() || now - openedAt < SWEEP_MIN_GAP_MS) {
      return false
    }
    dial(origin, slug, bearer, me = Line(), replacing = current)
    return true
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
    val me = Line()
    line.set(me)
    // Whatever was dialed before is not the line any more; its callbacks go quiet.
    socket.getAndSet(null)?.cancel()
    socket.set(dial(origin, slug, bearer, me, replacing = null))
  }

  /**
   * @param me the line this dial is (or, for a sweep, becomes once open).
   * @param replacing a sweep: the line to retire once this socket is open.
   *   Null means [me] is the line now, open or not.
   */
  private fun dial(origin: String, slug: String, bearer: String, me: Line, replacing: Line?): WebSocket {
    val req = Request.Builder()
      .url(mailboxUrl(origin, slug))
      .header("Authorization", "Bearer $bearer")
      .build()
    return client.newWebSocket(req, object : WebSocketListener() {
      private fun mine(): Boolean = line.get() === me

      override fun onOpen(webSocket: WebSocket, response: Response) {
        if (stopped.get()) {
          webSocket.cancel()
          return
        }
        val adopted = if (replacing != null) line.compareAndSet(replacing, me) else mine()
        if (!adopted) {
          webSocket.cancel()
          return
        }
        attempt.set(0)
        socket.getAndSet(webSocket)?.takeIf { it !== webSocket }?.cancel()
        openedAt = System.currentTimeMillis()
        up.set(true)
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
        if (!mine()) {
          return
        }
        up.set(false)
        onState(false)
        retry(origin, slug, bearer, null)
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        if (!mine()) {
          return
        }
        onError(t, response)
        up.set(false)
        onState(false)
        retry(origin, slug, bearer, response?.code)
      }
    })
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
