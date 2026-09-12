package com.gantree.cab

import com.gantree.cab.mailbox.THREAD_MAX
import com.gantree.cab.mailbox.capThread
import com.gantree.cab.mailbox.isDraftBubble
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Newest lines first until the JSON would pass this; one max photo still fits. */
const val THREAD_CACHE_CHARS_MAX = 4_000_000

/** Quiet time after the last thread change before it is written. */
const val THREAD_CACHE_SETTLE_MS = 750L

/** Whose thread: mailbox origin, crane slug, and the signed-in human (email; empty on the spike). */
data class ThreadRoom(
  val origin: String,
  val slug: String,
  val user: String,
)

/**
 * Settled bubbles only, like pendant `persistableThread`: a `sending` bubble
 * either lands (the transcript replays it) or never did; a draft is Kit
 * mid-sentence. Neither should greet you as history. `live` is a this-session
 * Compose key — a reload must not keep it.
 */
fun persistableThread(lines: List<ChatLine>): List<ChatLine> =
  lines.filterNot { it.pending || isDraftBubble(it.kind) }
    .map { if (it.live) it.copy(live = false) else it }

/**
 * Last thread on disk so launch paints it before the mailbox replays —
 * pendant `thread:<slug>:<sub>` in IndexedDB. One file, one room, one
 * human: anything else reads back empty. The mailbox transcript stays the
 * record; this is the paint.
 */
class ThreadCache(private val file: File) {
  fun read(room: ThreadRoom): List<ChatLine> {
    return try {
      if (file.isFile) decodeThread(file.readText(), room) else emptyList()
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun write(room: ThreadRoom, lines: List<ChatLine>) {
    try {
      file.parentFile?.mkdirs()
      val tmp = File(file.path + ".tmp")
      tmp.writeText(encodeThread(room, lines))
      Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    } catch (_: Exception) {
      /* a cache that cannot write is still a cache */
    }
  }
}

fun encodeThread(
  room: ThreadRoom,
  lines: List<ChatLine>,
  maxChars: Int = THREAD_CACHE_CHARS_MAX,
): String {
  val kept = ArrayList<JSONObject>()
  var used = 0
  for (line in persistableThread(lines).asReversed()) {
    val o = encodeLine(line)
    used += o.toString().length
    if (used > maxChars) {
      break
    }
    kept.add(o)
  }
  val arr = JSONArray()
  for (o in kept.asReversed()) {
    arr.put(o)
  }
  return JSONObject()
    .put("origin", room.origin)
    .put("slug", room.slug)
    .put("user", room.user)
    .put("lines", arr)
    .toString()
}

fun decodeThread(raw: String, room: ThreadRoom): List<ChatLine> {
  return try {
    val o = JSONObject(raw)
    if (ThreadRoom(o.optString("origin"), o.optString("slug"), o.optString("user")) != room) {
      return emptyList()
    }
    val arr = o.optJSONArray("lines") ?: return emptyList()
    val out = ArrayList<ChatLine>()
    for (i in 0 until arr.length()) {
      decodeLine(arr.optJSONObject(i) ?: continue)?.let { out.add(it) }
    }
    capThread(out, THREAD_MAX)
  } catch (_: Exception) {
    emptyList()
  }
}

private fun encodeLine(line: ChatLine): JSONObject {
  val o = JSONObject()
    .put("id", line.id)
    .put("you", line.fromYou)
    .put("text", line.text)
    .put("at", line.at)
  o.putOpt("kind", line.kind)
  o.putOpt("photo", line.photo)
  o.putOpt("seq", line.seq)
  o.putOpt("failed", line.failed)
  return o
}

private fun decodeLine(o: JSONObject): ChatLine? {
  val id = o.optString("id")
  if (id.isEmpty()) {
    return null
  }
  val kind = o.optString("kind").ifEmpty { null }
  if (isDraftBubble(kind)) {
    return null
  }
  return ChatLine(
    id = id,
    fromYou = o.optBoolean("you"),
    text = o.optString("text"),
    kind = kind,
    photo = o.optString("photo").takeIf { cachedPhotoOk(it) },
    at = o.optLong("at"),
    seq = if (o.isNull("seq")) null else o.optInt("seq").takeIf { it > 0 },
    failed = o.optString("failed").ifEmpty { null },
  )
}

/** Same shapes the wire admits; anything else on disk is not a photo. */
private fun cachedPhotoOk(url: String): Boolean =
  url.startsWith("data:image/") || url.startsWith("https://")
