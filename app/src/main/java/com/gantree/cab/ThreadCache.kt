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

/**
 * Last thread on disk so launch paints it before the mailbox replays.
 * One file, one room: a different origin or slug reads back empty.
 */
class ThreadCache(private val file: File) {
  fun read(origin: String, slug: String): List<ChatLine> {
    return try {
      if (file.isFile) decodeThread(file.readText(), origin, slug) else emptyList()
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun write(origin: String, slug: String, lines: List<ChatLine>) {
    try {
      file.parentFile?.mkdirs()
      val tmp = File(file.path + ".tmp")
      tmp.writeText(encodeThread(origin, slug, lines))
      Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    } catch (_: Exception) {
      /* a cache that cannot write is still a cache */
    }
  }
}

fun encodeThread(
  origin: String,
  slug: String,
  lines: List<ChatLine>,
  maxChars: Int = THREAD_CACHE_CHARS_MAX,
): String {
  val kept = ArrayList<JSONObject>()
  var used = 0
  for (line in lines.asReversed()) {
    if (isDraftBubble(line.kind)) {
      continue
    }
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
  return JSONObject().put("origin", origin).put("slug", slug).put("lines", arr).toString()
}

fun decodeThread(raw: String, origin: String, slug: String): List<ChatLine> {
  return try {
    val o = JSONObject(raw)
    if (o.optString("origin") != origin || o.optString("slug") != slug) {
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
    .put("pending", line.pending)
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
    pending = o.optBoolean("pending"),
    at = o.optLong("at"),
    seq = if (o.isNull("seq")) null else o.optInt("seq").takeIf { it > 0 },
    failed = o.optString("failed").ifEmpty { null },
  )
}

/** Same shapes the wire admits; anything else on disk is not a photo. */
private fun cachedPhotoOk(url: String): Boolean =
  url.startsWith("data:image/") || url.startsWith("https://")
