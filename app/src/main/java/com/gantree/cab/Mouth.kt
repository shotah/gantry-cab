package com.gantree.cab

import com.gantree.cab.mailbox.SlashCommand
import com.gantree.cab.mailbox.WireFrame
import com.gantree.cab.mailbox.faceRev
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

const val DRAFT_ID = "__draft__"

/** Phone TTL after the last typing frame. Crane refresh is ~4s. */
const val TYPING_TTL_MS = 6_000L

fun clearsTyping(kind: String?): Boolean =
  kind == "reply" || kind == "push" || kind == "error"

data class ChatLine(
  val id: String,
  val fromYou: Boolean,
  val text: String,
  val kind: String?,
  val photo: String? = null,
  val pending: Boolean = false,
  val at: Long = 0L,
)

class Mouth(
  private val now: () -> Long = { System.currentTimeMillis() },
) {
  private val _lines = MutableStateFlow<List<ChatLine>>(emptyList())
  private val _up = MutableStateFlow(false)
  private val _hint = MutableStateFlow("")
  private val _catalog = MutableStateFlow<List<SlashCommand>>(emptyList())
  private val _avatarRev = MutableStateFlow(0)
  private val _faceHint = MutableStateFlow("")
  private val _typingUntil = MutableStateFlow(0L)
  val lines: StateFlow<List<ChatLine>> = _lines
  val up: StateFlow<Boolean> = _up
  val hint: StateFlow<String> = _hint
  val catalog: StateFlow<List<SlashCommand>> = _catalog
  val avatarRev: StateFlow<Int> = _avatarRev
  val faceHint: StateFlow<String> = _faceHint
  val typingUntil: StateFlow<Long> = _typingUntil

  fun setUp(value: Boolean) {
    _up.value = value
    if (!value) {
      dropDraft()
      _typingUntil.value = 0L
    }
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
    _typingUntil.value = 0L
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
    if (frame.kind == "error") {
      _hint.value = frame.text?.trim().orEmpty().ifEmpty { "mailbox error" }
      _typingUntil.value = 0L
      return
    }
    if (frame.kind == "typing") {
      _typingUntil.value = now() + TYPING_TTL_MS
      return
    }
    if (frame.kind == "draft") {
      applyDraft(frame.text ?: "")
      return
    }
    if (frame.kind == "ack") {
      frame.id?.let { ack(it) }
      return
    }
    if (frame.kind == "allow" || frame.kind == "pin") {
      return
    }
    if (clearsTyping(frame.kind)) {
      _typingUntil.value = 0L
    }
    if (frame.kind == "reply") {
      dropDraft()
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
        at = now(),
      ),
    )
  }

  fun ack(id: String) {
    _lines.value = _lines.value.map { line ->
      if (line.id == id && line.pending) line.copy(pending = false) else line
    }
  }

  private fun applyDraft(text: String) {
    val rest = _lines.value.filter { it.id != DRAFT_ID }
    if (text.trim().isEmpty()) {
      _lines.value = rest
      return
    }
    _lines.value = (rest + ChatLine(DRAFT_ID, false, text, "draft", at = now())).takeLast(80)
  }

  private fun dropDraft() {
    val rest = _lines.value.filter { it.id != DRAFT_ID }
    if (rest.size != _lines.value.size) {
      _lines.value = rest
    }
  }
}
