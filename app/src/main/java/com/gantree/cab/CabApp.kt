package com.gantree.cab

import android.app.Application
import androidx.car.app.connection.CarConnection
import com.gantree.cab.drive.carConnectionAttached
import com.gantree.cab.mailbox.AuthApi
import com.gantree.cab.mailbox.AvatarApi
import com.gantree.cab.mailbox.PhoneContext
import com.gantree.cab.mailbox.ThemeApi
import com.gantree.cab.mailbox.WireFrame
import com.gantree.cab.mailbox.inbound
import com.gantree.cab.mailbox.shouldSpeak

class CabApp : Application() {
  lateinit var prefs: CabPrefs
    private set
  val mouth = Mouth()
  val auth = AuthApi()
  val avatar = AvatarApi()
  val theme = ThemeApi()
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

  override fun onCreate() {
    super.onCreate()
    prefs = CabPrefs(this)
    CarConnection(this).type.observeForever { type ->
      carAttached = carConnectionAttached(type)
    }
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
