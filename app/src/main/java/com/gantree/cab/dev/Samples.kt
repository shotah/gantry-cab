package com.gantree.cab.dev

import com.gantree.cab.ChatLine
import com.gantree.cab.DRAFT_ID

val SAMPLE_IDS = listOf("unsigned", "empty", "thread", "stream", "ping", "photo", "down")

data class SampleScene(
  val id: String,
  val slug: String = "kit",
  val email: String,
  val up: Boolean,
  val hint: String,
  val lines: List<ChatLine>,
  val typing: Boolean = false,
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
    "stream" -> SampleScene(
      id = key,
      email = "ada@example.com",
      up = true,
      hint = "pin ±12m this send",
      typing = true,
      lines = listOf(
        line("s1", true, "On the dock — is the gate still open?"),
        ChatLine(
          DRAFT_ID,
          false,
          "⏳ Gate's on the latch until 21:00. I'll ping you at…",
          "draft",
        ),
      ),
    )
    "photo" -> SampleScene(
      id = key,
      email = "ada@example.com",
      up = true,
      hint = "pin ±12m this send",
      lines = listOf(
        line("ph1", true, "This the right hatch?", photo = SAMPLE_PHOTO),
        line("ph2", false, "Yes — port side, yellow tape. Don't step the wet plate."),
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

private fun line(id: String, fromYou: Boolean, text: String, kind: String? = null, photo: String? = null) =
  ChatLine(id = id, fromYou = fromYou, text = text, kind = kind, photo = photo)

/** 1×1 JPEG so the photo sample is a real chat image, not an SVG mock. */
private const val SAMPLE_PHOTO =
  "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA="
