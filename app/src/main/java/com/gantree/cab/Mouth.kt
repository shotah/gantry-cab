package com.gantree.cab

import com.gantree.cab.mailbox.SlashCommand
import com.gantree.cab.mailbox.THREAD_MAX
import com.gantree.cab.mailbox.ThreadOrder
import com.gantree.cab.mailbox.WireFrame
import com.gantree.cab.mailbox.capThread
import com.gantree.cab.mailbox.describeSendError
import com.gantree.cab.mailbox.faceRev
import com.gantree.cab.mailbox.knownTheme
import com.gantree.cab.mailbox.placeInThread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

const val DRAFT_ID = "__draft__"

/** Phone TTL after the last typing frame. Crane refresh is ~4s. */
const val TYPING_TTL_MS = 6_000L

fun clearsTyping(kind: String?): Boolean =
  kind == "reply" || kind == "push" || kind == "error"

data class ChatLine(
  override val id: String,
  val fromYou: Boolean,
  val text: String,
  override val kind: String?,
  val photo: String? = null,
  val pending: Boolean = false,
  override val at: Long = 0L,
  override val seq: Int? = null,
  /** Mailbox refused it; sentence from [describeSendError]. */
  val failed: String? = null,
) : ThreadOrder

class Mouth(
  private val now: () -> Long = { System.currentTimeMillis() },
) {
  private val _lines = MutableStateFlow<List<ChatLine>>(emptyList())
  private val _up = MutableStateFlow(false)
  private val _hint = MutableStateFlow("")
  private val _catalog = MutableStateFlow<List<SlashCommand>>(emptyList())
  private val _avatarRev = MutableStateFlow(0)
  private val _backdropRev = MutableStateFlow(0)
  private val _roomTheme = MutableStateFlow("")
  private val _faceHint = MutableStateFlow("")
  private val _typingUntil = MutableStateFlow(0L)
  val lines: StateFlow<List<ChatLine>> = _lines
  val up: StateFlow<Boolean> = _up
  val hint: StateFlow<String> = _hint
  val catalog: StateFlow<List<SlashCommand>> = _catalog
  val avatarRev: StateFlow<Int> = _avatarRev
  val backdropRev: StateFlow<Int> = _backdropRev
  val roomTheme: StateFlow<String> = _roomTheme
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

  fun setBackdropRev(value: Int) {
    _backdropRev.value = value
  }

  fun setRoomTheme(value: String) {
    _roomTheme.value = value
  }

  fun add(line: ChatLine) {
    _lines.value = capThread(_lines.value + line, THREAD_MAX)
  }

  fun replace(lines: List<ChatLine>, up: Boolean, hint: String) {
    _lines.value = capThread(lines, THREAD_MAX)
    _up.value = up
    _hint.value = hint
    _catalog.value = emptyList()
    _typingUntil.value = 0L
    _backdropRev.value = 0
    _roomTheme.value = ""
  }

  /** @return true when a new turn was painted (not a restamp, draft, or control frame). */
  fun ingest(frame: WireFrame): Boolean {
    val rev = faceRev(frame.kind, frame.text)
    if (rev != null) {
      _avatarRev.value = rev
      return false
    }
    if (frame.kind == "backdrop") {
      frame.rev?.let { _backdropRev.value = it }
      return false
    }
    if (frame.kind == "theme") {
      when (val id = frame.theme) {
        null -> {}
        "" -> _roomTheme.value = ""
        else -> knownTheme(id)?.let { _roomTheme.value = it }
      }
      return false
    }
    if (frame.kind == "cmds") {
      _catalog.value = frame.commands.orEmpty()
      return false
    }
    if (frame.kind == "error") {
      // Refusal, not a turn: mark our bubble, never paint it as the crane.
      if (!fail(frame.id, describeSendError(frame.text))) {
        _hint.value = frame.text?.trim().orEmpty().ifEmpty { "mailbox error" }
      }
      _typingUntil.value = 0L
      return false
    }
    if (frame.kind == "typing") {
      _typingUntil.value = now() + TYPING_TTL_MS
      return false
    }
    if (frame.kind == "draft") {
      applyDraft(frame.text ?: "")
      return false
    }
    if (frame.kind == "ack") {
      frame.id?.let { ack(it) }
      return false
    }
    if (frame.kind == "allow" || frame.kind == "pin") {
      return false
    }
    if (clearsTyping(frame.kind)) {
      _typingUntil.value = 0L
    }
    val id = frame.id
    if (id != null) {
      val existing = _lines.value.find { it.id == id }
      if (existing != null) {
        val nextSeq = frame.seq ?: existing.seq
        val nextAt = frame.at ?: existing.at
        if (nextSeq != existing.seq || nextAt != existing.at) {
          commit(existing.copy(seq = nextSeq, at = nextAt))
        }
        return false
      }
    }
    if (frame.kind == "reply") {
      dropDraft()
    }
    val text = frame.text?.trim().orEmpty()
    val photo = frame.images?.firstOrNull()
    if (text.isEmpty() && photo == null && frame.kind != "push") {
      return false
    }
    commit(
      ChatLine(
        id = id ?: UUID.randomUUID().toString(),
        fromYou = frame.kind == "inbound",
        text = text.ifEmpty { if (photo != null) "" else "(ping)" },
        kind = frame.kind,
        photo = photo,
        at = frame.at ?: now(),
        seq = frame.seq,
      ),
    )
    return true
  }

  fun ack(id: String) {
    _lines.value = _lines.value.map { line ->
      if (line.id == id && line.pending) line.copy(pending = false) else line
    }
  }

  /**
   * A mailbox `error` names the frame it refused when it can (`id`); older
   * mailboxes and parse failures cannot, so fall back to your newest bubble
   * still marked sending. Same rule as pendant `failInThread`.
   * @return false when there was nothing of yours to mark.
   */
  fun fail(id: String?, why: String): Boolean {
    val lines = _lines.value
    val byId = if (id != null) lines.indexOfLast { it.fromYou && it.id == id } else -1
    val at = if (byId >= 0) byId else lines.indexOfLast { it.fromYou && it.pending }
    if (at < 0) {
      return false
    }
    _lines.value = lines.mapIndexed { i, line ->
      if (i == at) line.copy(pending = false, failed = why) else line
    }
    return true
  }

  private fun applyDraft(text: String) {
    val rest = _lines.value.filter { it.id != DRAFT_ID }
    if (text.trim().isEmpty()) {
      _lines.value = rest
      return
    }
    val prev = _lines.value.find { it.id == DRAFT_ID }
    _lines.value = capThread(
      placeInThread(
        rest,
        ChatLine(DRAFT_ID, false, text, "draft", at = prev?.at ?: now()),
      ),
      THREAD_MAX,
    )
  }

  private fun commit(line: ChatLine) {
    _lines.value = capThread(placeInThread(_lines.value, line), THREAD_MAX)
  }

  private fun dropDraft() {
    val rest = _lines.value.filter { it.id != DRAFT_ID }
    if (rest.size != _lines.value.size) {
      _lines.value = rest
    }
  }
}
