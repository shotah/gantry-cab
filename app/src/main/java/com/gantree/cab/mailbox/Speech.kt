package com.gantree.cab.mailbox

/**
 * Release → `stopListening()`. If the engine never answers, commit what was
 * heard after this so the `…` cannot hang. Pendant `LISTEN_END_MS`.
 */
const val LISTEN_END_MS = 2_000L

/** Hold-to-talk bar. Pendant `HoldState`. */
enum class HoldState { IDLE, LISTENING, FINISHING, BLOCKED }

fun holdLabel(state: HoldState): String = when (state) {
  HoldState.IDLE -> "Hold to talk"
  HoldState.LISTENING -> "Release to send"
  HoldState.FINISHING -> "…"
  HoldState.BLOCKED -> "Mic blocked"
}

private val RUNS = Regex("\\s+")

private fun tidy(s: String): String = s.trim().replace(RUNS, " ")

/**
 * Recognizers often report each longer hypothesis as a *new* final (`well`,
 * then `well I`, then `well I can send`…). Joining those is a stutter. A later
 * result that grows the previous one replaces it; a new sentence still appends.
 */
internal fun growsSpoken(earlier: String, later: String): Boolean {
  val a = tidy(earlier).lowercase()
  val b = tidy(later).lowercase()
  if (a.isEmpty() || b.isEmpty()) {
    return false
  }
  if (a == b) {
    return true
  }
  if (!b.startsWith(a)) {
    return false
  }
  val next = b[a.length]
  return next == ' ' || next in ".,!?;:'\")]"
}

internal fun foldSpoken(parts: MutableList<String>, next: String) {
  val t = tidy(next)
  if (t.isEmpty()) {
    return
  }
  val last = parts.lastOrNull()
  if (last == null) {
    parts.add(t)
    return
  }
  if (growsSpoken(last, t)) {
    parts[parts.size - 1] = t
    return
  }
  if (growsSpoken(t, last)) {
    return
  }
  parts.add(t)
}

/**
 * Best words so far: every final folded, plus the trailing interim so a release
 * that never gets a final still has something to send. Pendant `spokenFrom`.
 */
fun spokenFrom(finals: List<String>, interim: String = ""): String {
  val parts = mutableListOf<String>()
  for (f in finals) {
    foldSpoken(parts, f)
  }
  if (tidy(interim).isNotEmpty()) {
    foldSpoken(parts, interim)
  }
  return tidy(parts.joinToString(" "))
}

/**
 * One hold. The recognizer feeds partial and final hypotheses; [finish] hands
 * the words to `onDone` exactly once (release, end of speech, or the watchdog);
 * [abort] (slide-off) hands back "". Pendant `listen()` minus the browser object.
 */
class Utterance(private val onDone: (String) -> Unit) {
  private val finals = mutableListOf<String>()
  private var interim = ""
  var ended = false
    private set

  val heard: String
    get() = spokenFrom(finals, interim)

  fun partial(text: String) {
    if (!ended) {
      interim = text
    }
  }

  fun final(text: String) {
    if (ended) {
      return
    }
    foldSpoken(finals, text)
    interim = ""
  }

  fun finish() {
    if (ended) {
      return
    }
    ended = true
    onDone(heard)
  }

  fun abort() {
    if (ended) {
      return
    }
    finals.clear()
    interim = ""
    finish()
  }
}
