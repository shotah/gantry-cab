package com.gantree.cab.mailbox

import org.json.JSONArray
import org.json.JSONObject

data class Geo(
  val lat: Double,
  val lon: Double,
  val accuracyM: Double? = null,
)

data class PhoneContext(
  val at: String? = null,
  val tz: String? = null,
  val geo: Geo? = null,
)

data class WireFrame(
  val text: String? = null,
  val kind: String? = null,
  val id: String? = null,
  val since: String? = null,
  val images: List<String>? = null,
  val context: PhoneContext? = null,
  val commands: List<SlashCommand>? = null,
)

fun encodeFrame(frame: WireFrame): String {
  val o = JSONObject()
  frame.text?.let { o.put("text", it) }
  frame.kind?.let { o.put("kind", it) }
  frame.id?.let { o.put("id", it) }
  frame.since?.let { o.put("since", it) }
  frame.images?.takeIf { it.isNotEmpty() }?.let { urls ->
    val arr = JSONArray()
    for (url in urls) {
      arr.put(JSONObject().put("url", url))
    }
    o.put("images", arr)
  }
  frame.context?.let { ctx ->
    val c = JSONObject()
    ctx.at?.let { c.put("at", it) }
    ctx.tz?.let { c.put("tz", it) }
    ctx.geo?.let { g ->
      val geo = JSONObject().put("lat", g.lat).put("lon", g.lon)
      g.accuracyM?.let { geo.put("accuracy_m", it) }
      c.put("geo", geo)
    }
    if (c.length() > 0) {
      o.put("context", c)
    }
  }
  return o.toString()
}

fun parseFrame(raw: String): WireFrame? {
  if (raw == "ping" || raw == "pong") {
    return null
  }
  return try {
    val o = JSONObject(raw)
    val kind = o.optStringOrNull("kind")
    WireFrame(
      text = o.optStringOrNull("text"),
      kind = kind,
      id = o.optStringOrNull("id"),
      since = o.optStringOrNull("since"),
      images = o.optJSONArray("images")?.let { arr ->
        buildList {
          for (i in 0 until arr.length()) {
            val url = arr.optJSONObject(i)?.optString("url").orEmpty()
            if (url.isNotEmpty()) add(url)
          }
        }.ifEmpty { null }
      },
      commands = if (kind == "cmds") parseCommands(o.optJSONArray("commands")) else null,
    )
  } catch (_: Exception) {
    null
  }
}

fun inbound(text: String, id: String, context: PhoneContext?, images: List<String>? = null): WireFrame =
  WireFrame(
    text = text.takeIf { it.isNotEmpty() },
    kind = "inbound",
    id = id,
    context = context,
    images = images?.takeIf { it.isNotEmpty() },
  )

fun pinFrame(context: PhoneContext): WireFrame =
  WireFrame(kind = "pin", context = context)

fun ackSince(since: String): WireFrame = WireFrame(kind = "ack", since = since)

fun shouldSpeak(kind: String?): Boolean = kind == "reply" || kind == "push"

private fun JSONObject.optStringOrNull(key: String): String? {
  if (!has(key) || isNull(key)) {
    return null
  }
  val v = optString(key)
  return v.takeIf { it.isNotEmpty() }
}
