package com.gantree.cab

import android.app.Application
import androidx.car.app.connection.CarConnection
import com.gantree.cab.drive.carConnectionAttached
import com.gantree.cab.mailbox.AuthApi
import com.gantree.cab.mailbox.AvatarApi
import com.gantree.cab.mailbox.BlobCache
import com.gantree.cab.mailbox.PhoneContext
import com.gantree.cab.mailbox.ThemeApi
import com.gantree.cab.mailbox.WireFrame
import com.gantree.cab.mailbox.inbound
import com.gantree.cab.mailbox.shouldSpeak
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

class CabApp : Application() {
  lateinit var prefs: CabPrefs
    private set
  /** Face + wallpaper bytes remembered per room (`cacheDir/blobs`). */
  lateinit var avatar: AvatarApi
    private set
  /** Last thread for the room on disk (`cacheDir/thread.json`). */
  lateinit var thread: ThreadCache
    private set
  val mouth = Mouth()
  val auth = AuthApi()
  val theme = ThemeApi()
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  @Volatile
  var phoneResumed = false
  @Volatile
  var carAttached = false
    private set
  /** Cab's Auto conversation screen is in the foreground (not maps / launcher). */
  @Volatile
  var carThreadVisible = false
  /** Last JPEG from `/api/avatar`. Auto's Person icon; letter fallback when null. */
  @Volatile
  var face: ByteArray? = null
  /** A dev sample scene is on the thread; never write it over the room's cache. */
  @Volatile
  var sampleShown = false
  /** Whose thread is on screen. The cache reads and writes only under this. */
  @Volatile
  private var threadRoom: ThreadRoom? = null

  override fun onCreate() {
    super.onCreate()
    prefs = CabPrefs(this)
    avatar = AvatarApi(cache = BlobCache(File(cacheDir, "blobs")))
    thread = ThreadCache(File(cacheDir, "thread.json"))
    // Paint what we knew last time before any socket or GET answers.
    mouth.setRoomTheme(prefs.roomTheme(prefs.slug))
    openThread()
    scope.launch {
      mouth.lines
        .map(::persistableThread)
        .distinctUntilChanged()
        .collectLatest { lines ->
          delay(THREAD_CACHE_SETTLE_MS)
          val room = threadRoom
          if (room != null && !sampleShown) {
            thread.write(room, lines)
          }
        }
    }
    CarConnection(this).type.observeForever { type ->
      carAttached = carConnectionAttached(type)
    }
  }

  /**
   * Point the thread at the room in prefs. A different room or human drops
   * what is on screen (its transcript replays on connect) and paints that
   * room's last thread instead — pendant's `[roomSlug]` reset + `loadThread`.
   */
  fun openThread() {
    val room = ThreadRoom(prefs.origin, prefs.slug, prefs.email)
    if (room == threadRoom) {
      return
    }
    if (threadRoom != null) {
      mouth.clearThread()
    }
    threadRoom = room
    mouth.hydrate(thread.read(room))
  }
}

fun CabApp.outbound(text: String, context: PhoneContext?, images: List<String>? = null): WireFrame {
  val id = java.util.UUID.randomUUID().toString()
  val frame = inbound(text, id, context, images)
  mouth.add(
    ChatLine(
      id = id,
      fromYou = true,
      text = frame.text.orEmpty().ifEmpty { text },
      kind = "inbound",
      photo = images?.firstOrNull(),
      pending = true,
      at = System.currentTimeMillis(),
    ),
  )
  return frame
}

fun WireFrame.spoken(): Boolean = shouldSpeak(kind, replay)
