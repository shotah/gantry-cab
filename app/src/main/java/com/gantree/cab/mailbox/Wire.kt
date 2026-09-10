package com.gantree.cab.mailbox

import org.json.JSONArray
import org.json.JSONObject

data class Geo(
  val lat: Double,
  val lon: Double,
  val accuracyM: Double? = null,
  val altM: Double? = null,
  val heading: Double? = null,
  val speedMps: Double? = null,
)

data class BatteryHint(
  val pct: Int,
  val charging: Boolean,
)

data class PhoneContext(
  val at: String? = null,
  val tz: String? = null,
  val geo: Geo? = null,
  val battery: BatteryHint? = null,
  val net: String? = null,
  val surface: String? = null,
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

const val TEXT_BYTES_MAX = 8_000
const val TEXT_CHARS_MAX = TEXT_BYTES_MAX

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
      g.altM?.let { geo.put("alt_m", it) }
      g.heading?.let { geo.put("heading", it) }
      g.speedMps?.let { geo.put("speed_mps", it) }
      c.put("geo", geo)
    }
    ctx.battery?.let { b ->
      c.put("battery", JSONObject().put("pct", b.pct).put("charging", b.charging))
    }
    netOnWire(ctx.net)?.let { c.put("net", it) }
    surfaceOnWire(ctx.surface)?.let { c.put("surface", it) }
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
      text = capWireText(o.optStringOrNull("text")),
      kind = kind,
      id = o.optStringOrNull("id"),
      since = o.optStringOrNull("since"),
      images = o.optJSONArray("images")?.let { arr ->
        buildList {
          for (i in 0 until arr.length()) {
            val url = arr.optJSONObject(i)?.optString("url").orEmpty()
            if (acceptInboundImage(url)) add(url)
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
    text = capWireText(text.takeIf { it.isNotEmpty() }),
    kind = "inbound",
    id = id,
    context = context,
    images = images?.takeIf { it.isNotEmpty() },
  )

fun pinFrame(context: PhoneContext): WireFrame =
  WireFrame(kind = "pin", context = context)

fun ackSince(since: String): WireFrame = WireFrame(kind = "ack", since = since)

fun shouldSpeak(kind: String?): Boolean = kind == "reply" || kind == "push"

fun capWireText(text: String?): String? {
  val t = text ?: return null
  return capUtf8(t, TEXT_BYTES_MAX)
}

fun capUtf8(text: String, maxBytes: Int = TEXT_BYTES_MAX): String {
  val bytes = text.toByteArray(Charsets.UTF_8)
  if (bytes.size <= maxBytes) {
    return text
  }
  var n = maxBytes
  while (n > 0 && n < bytes.size && (bytes[n].toInt() and 0xC0) == 0x80) {
    n--
  }
  return String(bytes, 0, n, Charsets.UTF_8)
}

fun acceptInboundImage(url: String): Boolean {
  if (url.isEmpty()) {
    return false
  }
  if (url.startsWith("data:image/")) {
    return decodeDataUrl(url) != null
  }
  return url.startsWith("https://") && url.length <= 2_048
}

fun netOnWire(net: String?): String? =
  net.takeIf { it == "wifi" || it == "cellular" || it == "unknown" }

fun surfaceOnWire(surface: String?): String? =
  surface.takeIf { it == "car" || it == "phone" }

fun surfaceHint(carAttached: Boolean): String = if (carAttached) "car" else "phone"

fun batteryHint(pct: Int, charging: Boolean): BatteryHint? {
  if (pct !in 0..100) {
    return null
  }
  return BatteryHint(pct, charging)
}

fun netHint(wifi: Boolean, cellular: Boolean): String = when {
  wifi -> "wifi"
  cellular -> "cellular"
  else -> "unknown"
}

fun notifyBody(text: String?, hasPhoto: Boolean): String {
  val t = text?.trim().orEmpty()
  if (t.isNotEmpty()) {
    return t
  }
  return if (hasPhoto) "Photo" else "ping"
}

private fun JSONObject.optStringOrNull(key: String): String? {
  if (!has(key) || isNull(key)) {
    return null
  }
  val v = optString(key)
  return v.takeIf { it.isNotEmpty() }
}
