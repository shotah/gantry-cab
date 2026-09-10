package com.gantree.cab

import android.app.Application
import com.gantree.cab.mailbox.AuthApi
import com.gantree.cab.mailbox.AvatarApi
import com.gantree.cab.mailbox.PhoneContext
import com.gantree.cab.mailbox.WireFrame
import com.gantree.cab.mailbox.inbound
import com.gantree.cab.mailbox.shouldSpeak

class CabApp : Application() {
  lateinit var prefs: CabPrefs
    private set
  val mouth = Mouth()
  val auth = AuthApi()
  val avatar = AvatarApi()

  override fun onCreate() {
    super.onCreate()
    prefs = CabPrefs(this)
  }
}

fun CabApp.outbound(text: String, context: PhoneContext?, images: List<String>? = null): WireFrame {
  val id = java.util.UUID.randomUUID().toString()
  val frame = inbound(text, id, context, images)
  mouth.add(
    ChatLine(
      id = id,
      fromYou = true,
      text = text,
      kind = "inbound",
      photo = images?.firstOrNull(),
    ),
  )
  return frame
}

fun WireFrame.spoken(): Boolean = shouldSpeak(kind)
