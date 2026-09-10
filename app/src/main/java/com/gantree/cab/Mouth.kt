package com.gantree.cab

import com.gantree.cab.mailbox.SlashCommand
import com.gantree.cab.mailbox.WireFrame
import com.gantree.cab.mailbox.faceRev
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

data class ChatLine(
  val id: String,
  val fromYou: Boolean,
  val text: String,
  val kind: String?,
  val photo: String? = null,
)

class Mouth {
  private val _lines = MutableStateFlow<List<ChatLine>>(emptyList())
  private val _up = MutableStateFlow(false)
  private val _hint = MutableStateFlow("")
  private val _catalog = MutableStateFlow<List<SlashCommand>>(emptyList())
  private val _avatarRev = MutableStateFlow(0)
  private val _faceHint = MutableStateFlow("")
  val lines: StateFlow<List<ChatLine>> = _lines
  val up: StateFlow<Boolean> = _up
  val hint: StateFlow<String> = _hint
  val catalog: StateFlow<List<SlashCommand>> = _catalog
  val avatarRev: StateFlow<Int> = _avatarRev
  val faceHint: StateFlow<String> = _faceHint

  fun setUp(value: Boolean) {
    _up.value = value
  }

  fun setHint(value: String) {
    _hint.value = value
  }

  fun setFaceHint(value: String) {
    _faceHint.value = value
  }

  fun setAvatarRev(value: Int) {
    _avatarRev.value = value
  }

  fun add(line: ChatLine) {
    _lines.value = (_lines.value + line).takeLast(80)
  }

  fun replace(lines: List<ChatLine>, up: Boolean, hint: String) {
    _lines.value = lines.takeLast(80)
    _up.value = up
    _hint.value = hint
    _catalog.value = emptyList()
  }

  fun ingest(frame: WireFrame) {
    val rev = faceRev(frame.kind, frame.text)
    if (rev != null) {
      _avatarRev.value = rev
      return
    }
    if (frame.kind == "cmds") {
      _catalog.value = frame.commands.orEmpty()
      return
    }
    if (frame.kind == "ack" || frame.kind == "typing" || frame.kind == "draft" || frame.kind == "allow" || frame.kind == "pin") {
      return
    }
    val text = frame.text?.trim().orEmpty()
    val photo = frame.images?.firstOrNull()
    if (text.isEmpty() && photo == null && frame.kind != "push") {
      return
    }
    add(
      ChatLine(
        id = frame.id ?: UUID.randomUUID().toString(),
        fromYou = frame.kind == "inbound",
        text = text.ifEmpty { if (photo != null) "" else "(ping)" },
        kind = frame.kind,
        photo = photo,
      ),
    )
  }
}
