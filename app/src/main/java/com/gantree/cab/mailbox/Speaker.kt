package com.gantree.cab.mailbox

/**
 * Kit's pocket voice as the phone sees it. Pendant `lib/phone/speaker.ts`:
 * the shell paints these so a silent reply is never a mystery.
 */
enum class SpeakPhase { IDLE, FETCHING, PLAYING }

/** Why a reply stayed silent. [EMPTY] (nothing speakable) is not worth a line. */
enum class SpeakFail { EMPTY, OFFLINE, NO_VOICE, UNAUTHORIZED, BUSY, VENDOR, PLAY }

/** `/api/tts` status → reason. 404 is the Worker not offering voice; 5xx is Google. */
fun speakFailFromStatus(status: Int): SpeakFail = when (status) {
  404 -> SpeakFail.NO_VOICE
  401, 403 -> SpeakFail.UNAUTHORIZED
  429 -> SpeakFail.BUSY
  else -> SpeakFail.VENDOR
}

/** Header words next to `Live`. "" when the speaker is quiet. */
fun speakStatus(phase: SpeakPhase): String = when (phase) {
  SpeakPhase.FETCHING -> "voice…"
  SpeakPhase.PLAYING -> "speaking"
  SpeakPhase.IDLE -> ""
}

/** Hold-bar words while idle and Kit is talking. "" hands the bar back to "Hold to talk". */
fun speakBarLabel(phase: SpeakPhase): String = when (phase) {
  SpeakPhase.FETCHING -> "Fetching voice…"
  SpeakPhase.PLAYING -> "Speaking · hold to cut in"
  SpeakPhase.IDLE -> ""
}

/** One line under the header when a reply stayed silent. "" for nothing-to-say. */
fun speakFailHint(reason: SpeakFail): String = when (reason) {
  SpeakFail.NO_VOICE -> "Kit's voice is off on this Worker (no TTS key, or VOICE=off)."
  SpeakFail.UNAUTHORIZED -> "Kit's voice: sign in again."
  SpeakFail.BUSY -> "Kit's voice: too many requests, try again in a moment."
  SpeakFail.VENDOR -> "Kit's voice failed at Google. Check the Cloud Text-to-Speech API and the key restriction."
  SpeakFail.OFFLINE -> "Kit's voice: could not reach the Worker."
  SpeakFail.PLAY -> "Kit's voice: the phone would not play it. Hold again."
  SpeakFail.EMPTY -> ""
}

/** Header line under the crane name: socket first, then Kit's voice, then typing. */
fun liveStatus(up: Boolean, typing: Boolean, phase: SpeakPhase): String = when {
  !up -> "Offline"
  phase != SpeakPhase.IDLE -> "Live · ${speakStatus(phase)}"
  typing -> "Live · typing…"
  else -> "Live"
}

/**
 * Handheld speaker gate (pendant `awaitingVoice`): only the live `reply` to a
 * hold this phone armed, once. `push`, `replay`, a sibling's `inbound`, a
 * restamp of a bubble already painted, and typed turns all stay quiet. The car
 * keeps its own engine ([shouldSpeak]).
 */
fun speaksReply(kind: String?, replay: Boolean, fresh: Boolean, armed: Boolean): Boolean =
  armed && fresh && !replay && kind == "reply"

/** A refusal answers the turn you were waiting on; there is nothing to read. */
fun disarmsVoice(kind: String?): Boolean = kind == "error"

/**
 * The hold bar replaces compose only when the Worker publishes voice
 * (`/api/auth/config` `voice: true`) **and** the human flipped the header mic.
 * A remembered `on` against a Worker with no voice still types.
 */
fun voiceBarShown(offered: Boolean, on: Boolean): Boolean = offered && on
