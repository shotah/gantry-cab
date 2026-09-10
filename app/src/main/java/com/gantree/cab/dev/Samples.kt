package com.gantree.cab.dev

import com.gantree.cab.ChatLine

val SAMPLE_IDS = listOf("unsigned", "empty", "thread", "ping", "down")

data class SampleScene(
  val id: String,
  val slug: String = "kit",
  val email: String,
  val up: Boolean,
  val hint: String,
  val lines: List<ChatLine>,
)

fun parseSample(raw: String?): String? {
  val id = raw?.trim()?.lowercase().orEmpty()
  return id.takeIf { it in SAMPLE_IDS }
}

fun sampleScene(id: String): SampleScene? {
  val key = parseSample(id) ?: return null
  return when (key) {
    "unsigned" -> SampleScene(
      id = key,
      email = "",
      up = false,
      hint = "Sign in with Google to talk",
      lines = emptyList(),
    )
    "empty" -> SampleScene(
      id = key,
      email = "ada@example.com",
      up = true,
      hint = "GPS attaches on send if the OS allows it.",
      lines = emptyList(),
    )
    "ping" -> SampleScene(
      id = key,
      email = "ada@example.com",
      up = true,
      hint = "pin ±12m this send",
      lines = listOf(
        line("p1", false, "20:40 — still on the dock? Gate latches in twenty.", "push"),
        line("p2", true, "Walking back."),
        line("p3", false, "I'll hush."),
      ),
    )
    "down" -> SampleScene(
      id = key,
      email = "ada@example.com",
      up = false,
      hint = "socket down — reconnecting",
      lines = listOf(
        line("d1", true, "On the dock — is the gate still open?"),
      ),
    )
    else -> SampleScene(
      id = "thread",
      email = "ada@example.com",
      up = true,
      hint = "pin ±12m this send",
      lines = listOf(
        line("t1", true, "On the dock — is the gate still open?"),
        line("t2", false, "Gate's on the latch until 21:00. I'll ping you at 20:40 if you're still out."),
        line("t3", true, "Leave by 20:50 then."),
        line("t4", false, "Leave-by 20:50. Pin is this-send, ±12m."),
      ),
    )
  }
}

private fun line(id: String, fromYou: Boolean, text: String, kind: String? = null) =
  ChatLine(id = id, fromYou = fromYou, text = text, kind = kind)
