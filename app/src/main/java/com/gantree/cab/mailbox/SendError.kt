package com.gantree.cab.mailbox

/**
 * Mailbox `kind: "error"` frames carry a short wire token in `text` (`rate`,
 * `too large`, `bad frame`). Paint a sentence under your own bubble. Same
 * strings as pendant `lib/phone/sendError.ts`.
 */
fun describeSendError(text: String?): String = when (text?.trim()?.lowercase()) {
  "rate" -> "Not sent — too much too fast. Wait a minute, then try again."
  "too large" -> "Not sent — too big for the room."
  else -> "Not sent."
}

/** Photo failed before the wire: the shrink ladder bottomed out, or the decoder gave up. */
fun describePhotoError(error: String): String =
  if (error == "too large") {
    "Photo not sent — still too big after shrinking."
  } else {
    "Photo not sent — couldn't read that image."
  }

/** `jpegFromUri` throws pendant's messages; fold them to the two wire-side tokens. */
fun photoErrorToken(message: String?): String =
  if (message.orEmpty().contains("too large")) "too large" else "bad photo"
