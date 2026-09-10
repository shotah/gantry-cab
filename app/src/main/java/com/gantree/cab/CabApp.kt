package com.gantree.cab

import android.app.Application
import com.gantree.cab.mailbox.AuthApi
import com.gantree.cab.mailbox.PhoneContext
import com.gantree.cab.mailbox.WireFrame
import com.gantree.cab.mailbox.inbound
import com.gantree.cab.mailbox.shouldSpeak
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

data class ChatLine(
  val id: String,
  val fromYou: Boolean,
  val text: String,
  val kind: String?,
)

class Mouth {
  private val _lines = MutableStateFlow<List<ChatLine>>(emptyList())
  private val _up = MutableStateFlow(false)
  private val _hint = MutableStateFlow("")
  val lines: StateFlow<List<ChatLine>> = _lines
  val up: StateFlow<Boolean> = _up
  val hint: StateFlow<String> = _hint

  fun setUp(value: Boolean) {
    _up.value = value
  }

  fun setHint(value: String) {
    _hint.value = value
  }

  fun add(line: ChatLine) {
    _lines.value = (_lines.value + line).takeLast(80)
  }

  fun ingest(frame: WireFrame) {
    if (frame.kind == "ack" || frame.kind == "cmds" || frame.kind == "typing" || frame.kind == "draft" || frame.kind == "allow") {
      return
    }
    val text = frame.text?.trim().orEmpty()
    if (text.isEmpty() && frame.kind != "push") {
      return
    }
    add(
      ChatLine(
        id = frame.id ?: UUID.randomUUID().toString(),
        fromYou = frame.kind == "inbound",
        text = text.ifEmpty { "(ping)" },
        kind = frame.kind,
      ),
    )
  }
}

class CabApp : Application() {
  lateinit var prefs: CabPrefs
    private set
  val mouth = Mouth()
  val auth = AuthApi()

  override fun onCreate() {
    super.onCreate()
    prefs = CabPrefs(this)
  }
}

fun CabApp.outbound(text: String, context: PhoneContext?): WireFrame {
  val id = UUID.randomUUID().toString()
  val frame = inbound(text, id, context)
  mouth.add(ChatLine(id = id, fromYou = true, text = text, kind = "inbound"))
  return frame
}

fun WireFrame.spoken(): Boolean = shouldSpeak(kind)
