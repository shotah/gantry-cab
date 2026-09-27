package com.gantree.cab.mailbox

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Goals board (`kind: "aims"`). Crane only; the mailbox keeps the latest and
 * replays it on connect. Whole board every time; empty `aims` clears. Not a
 * turn. Same shape and caps as pendant `lib/mailbox/aims.ts`.
 */

const val AIMS_MAX = 5
const val AIM_DAYS_MAX = 14
const val AIM_WEEKS_MAX = 13
const val AIM_LINKS_MAX = 3
const val AIM_SENTENCE_MAX = 200

private val AREA_RE = Regex("^[a-z0-9_-]{1,48}$")
private val DAY_RE = Regex("^\\d{4}-\\d{2}-\\d{2}$")
private val NOTES = setOf("nudged", "asked", "offered", "praised", "quiet", "")

data class AimDay(val day: String, val score: Int, val events: List<Long>)

data class AimMetric(val metric: String, val mean: Double, val unit: String, val n: Int)

data class AimWeek(val start: String, val mean: Double, val up: Int, val against: Int, val metrics: List<AimMetric>)

data class AimBlock(val days: Int, val up: Int, val against: Int, val mean: Double, val pct: Double)

data class AimEffect(val a: String, val b: String, val metric: String, val r: Double, val n: Int)

data class AimLink(val a: String, val b: String, val r: Double, val n: Int)

data class Aim(
  val area: String,
  val sentence: String,
  val rating30: Double,
  val sum7: Int,
  val streak: Int,
  val note: String,
  val noteAt: String? = null,
  val days: List<AimDay> = emptyList(),
  val weeks: List<AimWeek> = emptyList(),
  val slope: Double? = null,
  val block: AimBlock? = null,
  val effect: AimEffect? = null,
)

data class AimsBoard(
  val aims: List<Aim> = emptyList(),
  val links: List<AimLink> = emptyList(),
) {
  val isEmpty: Boolean get() = aims.isEmpty()
}

/** Whole frame → board. Missing `aims` array is junk (null); `[]` is a real clear. */
fun parseAims(o: JSONObject): AimsBoard? {
  val arr = o.optJSONArray("aims") ?: return null
  val aims = buildList {
    for (i in 0 until arr.length()) {
      if (size >= AIMS_MAX) {
        break
      }
      val aim = parseAim(arr.optJSONObject(i) ?: continue) ?: continue
      add(aim)
    }
  }
  val areas = aims.mapTo(HashSet()) { it.area }
  return AimsBoard(aims = aims, links = parseLinks(o.optJSONArray("links"), areas))
}

internal fun parseAim(o: JSONObject): Aim? {
  val area = o.optString("area").trim()
  if (!AREA_RE.matches(area)) {
    return null
  }
  val sentence = o.optString("sentence").trim().take(AIM_SENTENCE_MAX)
  if (sentence.isEmpty()) {
    return null
  }
  val rating30 = finite(o.opt("rating30")) ?: return null
  if (rating30 < -3.0 || rating30 > 3.0) {
    return null
  }
  val sum7 = whole(o.opt("sum7")) ?: return null
  val streak = whole(o.opt("streak")) ?: return null
  val note = o.optString("note").trim()
  if (note !in NOTES) {
    return null
  }
  val days = parseDays(o.optJSONArray("days") ?: return null)
  return Aim(
    area = area,
    sentence = sentence,
    rating30 = rating30,
    sum7 = sum7,
    streak = streak,
    note = note,
    noteAt = o.optString("note_at").takeIf { DAY_RE.matches(it) },
    days = days,
    weeks = parseWeeks(o.optJSONArray("weeks")),
    slope = finite(o.opt("slope")),
    block = parseBlock(o.optJSONObject("block")),
    effect = parseEffect(o.optJSONObject("effect")),
  )
}

private fun parseDays(arr: JSONArray): List<AimDay> = buildList {
  for (i in 0 until arr.length()) {
    if (size >= AIM_DAYS_MAX) {
      break
    }
    val d = arr.optJSONObject(i) ?: continue
    val day = d.optString("day")
    if (!DAY_RE.matches(day)) {
      continue
    }
    val score = whole(d.opt("score")) ?: continue
    add(AimDay(day = day, score = score, events = parseIds(d.optJSONArray("events"))))
  }
}

private fun parseIds(arr: JSONArray?): List<Long> {
  if (arr == null) {
    return emptyList()
  }
  return buildList {
    for (i in 0 until arr.length()) {
      val n = jsonWholeNumber(arr.opt(i)) ?: continue
      if (n > 0) add(n)
    }
  }
}

private fun parseWeeks(arr: JSONArray?): List<AimWeek> {
  if (arr == null) {
    return emptyList()
  }
  return buildList {
    for (i in 0 until arr.length()) {
      if (size >= AIM_WEEKS_MAX) {
        break
      }
      val w = arr.optJSONObject(i) ?: continue
      val start = w.optString("start")
      if (!DAY_RE.matches(start)) {
        continue
      }
      val mean = finite(w.opt("mean")) ?: continue
      val up = whole(w.opt("up")) ?: continue
      val against = whole(w.opt("against")) ?: continue
      add(AimWeek(start = start, mean = mean, up = up, against = against, metrics = parseMetrics(w.optJSONArray("metrics"))))
    }
  }
}

private fun parseMetrics(arr: JSONArray?): List<AimMetric> {
  if (arr == null) {
    return emptyList()
  }
  return buildList {
    for (i in 0 until arr.length()) {
      val m = arr.optJSONObject(i) ?: continue
      val metric = m.optString("metric").trim()
      if (metric.isEmpty()) {
        continue
      }
      val mean = finite(m.opt("mean")) ?: continue
      val n = whole(m.opt("n")) ?: continue
      add(AimMetric(metric = metric, mean = mean, unit = m.optString("unit").trim(), n = n))
    }
  }
}

/** A half-formed block is dropped whole (pendant: "a missing line means too early"). */
private fun parseBlock(o: JSONObject?): AimBlock? {
  if (o == null) {
    return null
  }
  val days = whole(o.opt("days")) ?: return null
  val up = whole(o.opt("up")) ?: return null
  val against = whole(o.opt("against")) ?: return null
  val mean = finite(o.opt("mean")) ?: return null
  val pct = finite(o.opt("pct")) ?: return null
  if (days <= 0) {
    return null
  }
  return AimBlock(days = days, up = up, against = against, mean = mean, pct = pct)
}

private fun parseEffect(o: JSONObject?): AimEffect? {
  if (o == null) {
    return null
  }
  val metric = o.optString("metric").trim()
  if (metric.isEmpty()) {
    return null
  }
  val r = finite(o.opt("r")) ?: return null
  val n = whole(o.opt("n")) ?: return null
  return AimEffect(a = o.optString("a").trim(), b = o.optString("b").trim(), metric = metric, r = r, n = n)
}

/** Cross-aim lines. Both ends must be areas on this board; `a == b` is dropped. Cap 3. */
private fun parseLinks(arr: JSONArray?, areas: Set<String>): List<AimLink> {
  if (arr == null) {
    return emptyList()
  }
  return buildList {
    for (i in 0 until arr.length()) {
      if (size >= AIM_LINKS_MAX) {
        break
      }
      val l = arr.optJSONObject(i) ?: continue
      val a = l.optString("a").trim()
      val b = l.optString("b").trim()
      if (a == b || a !in areas || b !in areas) {
        continue
      }
      val r = finite(l.opt("r")) ?: continue
      val n = whole(l.opt("n")) ?: continue
      add(AimLink(a = a, b = b, r = r, n = n))
    }
  }
}

private fun whole(raw: Any?): Int? {
  val n = jsonWholeNumber(raw) ?: return null
  if (n < Int.MIN_VALUE || n > Int.MAX_VALUE) {
    return null
  }
  return n.toInt()
}

private fun finite(raw: Any?): Double? = when (raw) {
  is Number -> raw.toDouble().takeIf { it.isFinite() }
  else -> null
}

/** `+1.4` / `-0.4` / `0`. One decimal for means and ratings, none for whole numbers. */
fun signed(n: Double, decimals: Int = 1): String {
  if (n == 0.0) {
    return "0"
  }
  val body = String.format(Locale.US, "%.${decimals}f", n)
  return if (n > 0) "+$body" else body
}

fun signed(n: Int): String = if (n > 0) "+$n" else n.toString()

/** The `[aims]` stamp line, word for word: `30d +1.4 · 7d +6 · streak 2 · asked`. */
fun statsLine(aim: Aim): String {
  val parts = mutableListOf("30d ${signed(aim.rating30)}", "7d ${signed(aim.sum7)}", "streak ${aim.streak}")
  if (aim.note.isNotEmpty()) {
    parts.add(aim.note)
  }
  return parts.joinToString(" · ")
}

/** `slope +0.3/wk · block 4/10 (40%) · weight r -0.42 (n 9)`. Empty when nothing has formed yet. */
fun trendLine(aim: Aim): String {
  val parts = mutableListOf<String>()
  aim.slope?.let { parts.add("slope ${signed(it)}/wk") }
  aim.block?.let { parts.add("block ${it.up}/${it.days} (${Math.round(it.pct * 100)}%)") }
  aim.effect?.let { parts.add("${it.metric} r ${signed(it.r, 2)} (n ${it.n})") }
  return parts.joinToString(" · ")
}

/** The `/aims` footer line: `training → next-day weight r +0.38 (n 12)`. */
fun linkLine(link: AimLink): String =
  "${link.a} → next-day ${link.b} r ${signed(link.r, 2)} (n ${link.n})"

/**
 * Header chip text. The number is a call to action, not the board size:
 * only aims that differ from the board last opened. Nothing changed → bare.
 */
fun goalsLabel(changed: Int): String = if (changed > 0) "goals ($changed)" else "goals"

/**
 * One row as the human last saw it, area → canonical JSON. Stored on the
 * device (`aimsSeen`, same idea as `pendant.aimsSeen`) so a replay of the
 * same board on reconnect badges nothing.
 */
fun seenAims(board: AimsBoard): Map<String, String> =
  board.aims.associate { it.area to aimRow(it) }

/** New, changed, or gone since the last open. A never-seen board counts whole. */
fun changedAims(board: AimsBoard, seen: Map<String, String>): Int = changedRows(seenAims(board), seen)

fun encodeSeenAims(seen: Map<String, String>): String = encodeSeenRows(seen)

/** Junk → empty (never-seen). Only string rows under area-shaped keys survive. */
fun parseSeenAims(raw: String?): Map<String, String> = parseSeenRows(raw) { AREA_RE.matches(it) }

/**
 * Shared by every board (pendant `lib/phone/boardSeen.ts`): key → row as
 * last seen. The count is a call to action, not the board size.
 */
fun changedRows(now: Map<String, String>, seen: Map<String, String>): Int {
  val differ = now.count { (key, row) -> seen[key] != row }
  val gone = seen.keys.count { it !in now }
  return differ + gone
}

fun encodeSeenRows(seen: Map<String, String>): String {
  val o = JSONObject()
  for ((key, row) in seen) {
    o.put(key, row)
  }
  return o.toString()
}

fun parseSeenRows(raw: String?, keyOk: (String) -> Boolean): Map<String, String> {
  if (raw.isNullOrBlank()) {
    return emptyMap()
  }
  return try {
    val o = JSONObject(raw)
    buildMap {
      for (key in o.keys()) {
        val row = o.opt(key)
        if (keyOk(key) && row is String) {
          put(key, row)
        }
      }
    }
  } catch (_: Exception) {
    emptyMap()
  }
}

/** Stable text for "did this row change": what the card paints, in field order. */
internal fun aimRow(aim: Aim): String {
  val o = JSONObject()
  o.put("sentence", aim.sentence)
  o.put("rating30", aim.rating30)
  o.put("sum7", aim.sum7)
  o.put("streak", aim.streak)
  o.put("note", aim.note)
  o.put("note_at", aim.noteAt ?: "")
  o.put("days", JSONArray(aim.days.map { "${it.day}:${it.score}:${it.events.size}" }))
  o.put("weeks", JSONArray(aim.weeks.map { "${it.start}:${it.mean}:${it.up}:${it.against}" }))
  o.put("slope", aim.slope ?: JSONObject.NULL)
  o.put("block", aim.block?.let { "${it.days}:${it.up}:${it.against}:${it.mean}:${it.pct}" } ?: "")
  o.put("effect", aim.effect?.let { "${it.metric}:${it.r}:${it.n}" } ?: "")
  return o.toString()
}

/** Every ask is a visible turn. */
fun askAim(area: String): String = "/aims $area"

const val ASK_AIMS_REPORT = "/aims"
const val ASK_AIMS_RUBRIC = "/aims rubric"
