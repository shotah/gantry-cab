package com.gantree.cab.mailbox

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * Tasks board (`kind: "todo"`). Crane only; the mailbox keeps the latest and
 * replays it on connect. Whole list every time, **oldest first**; empty
 * clears. Not a turn. The one write the phone makes is the checkbox, and it
 * is an ordinary visible turn: `/todo done <id>`. Same shape and caps as
 * pendant `lib/mailbox/todo.ts` / crane `docs/tasks.md` §4.4.
 */

/** The crane does not cap the frame; the phone keeps this many and drops the rest. */
const val TODO_MAX = 100

/** Runes (code points), pendant `TODO_TEXT_MAX`. */
const val TODO_TEXT_MAX = 240

/** Past this many open the footer says it is a pocket list, not a tracker. */
const val TODO_POCKET_MAX = 10

const val TODO_LIST_COMMAND = "/todo"

private val SLUG_RE = Regex("^[a-z0-9][a-z0-9_-]*$")
private val DATE_RE = Regex("^\\d{4}-\\d{2}-\\d{2}$")

data class TodoRow(
  /** Memory row id; what the checkbox sends back. Changes when the words change. */
  val id: Long,
  /** Key after `todo/`; the identity across rewrites. */
  val slug: String,
  val text: String,
  /** Local `YYYY-MM-DD` last written. The phone computes the age. */
  val at: String,
)

/** Whole frame → rows. Missing `todo` array is junk (null); `[]` is a real clear. */
fun parseTodo(o: JSONObject): List<TodoRow>? {
  val arr = o.optJSONArray("todo") ?: return null
  val ids = HashSet<Long>()
  val slugs = HashSet<String>()
  return buildList {
    for (i in 0 until arr.length()) {
      if (size >= TODO_MAX) {
        break
      }
      val row = parseTodoRow(arr.optJSONObject(i) ?: continue) ?: continue
      // Repeating either key drops the row, not the list.
      if (!ids.add(row.id) || !slugs.add(row.slug)) {
        continue
      }
      add(row)
    }
  }
}

internal fun parseTodoRow(o: JSONObject): TodoRow? {
  val id = jsonWholeNumber(o.opt("id")) ?: return null
  if (id <= 0) {
    return null
  }
  val slug = o.optString("slug").trim()
  if (!SLUG_RE.matches(slug)) {
    return null
  }
  val text = collapseWhitespace(o.optString("text"))
  if (text.isEmpty()) {
    return null
  }
  val at = o.optString("at").trim()
  if (!DATE_RE.matches(at)) {
    return null
  }
  return TodoRow(id = id, slug = slug, text = capRunes(text, TODO_TEXT_MAX), at = at)
}

private fun collapseWhitespace(s: String): String =
  s.split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")

private fun capRunes(s: String, max: Int): String {
  if (s.codePointCount(0, s.length) <= max) {
    return s
  }
  return s.substring(0, s.offsetByCodePoints(0, max))
}

/**
 * Priority is a marker leading the words — `!!` urgent, `!` high, none
 * normal (`"!! file the extension"`) — set by the crane (theirs, or its read
 * of the stakes). It rides inside `text`, so [TodoRow.text] keeps it (the
 * seen-badge keys on the raw words) and the sheet reads it out here.
 */
enum class TodoPriority(val rank: Int, val tag: String?) {
  URGENT(2, "urgent"),
  HIGH(1, "high"),
  NORMAL(0, null),
}

/** Marker, a space, then the task. `!!!`, a glued `!!file`, or a bare `!!` are words, not a marker. */
private val PRIORITY_RE = Regex("^(!!?) (?!!)(.+)$")

fun todoPriority(text: String): TodoPriority = when (PRIORITY_RE.find(text)?.groupValues?.get(1)) {
  "!!" -> TodoPriority.URGENT
  "!" -> TodoPriority.HIGH
  else -> TodoPriority.NORMAL
}

/** The task without its marker; what the row paints. */
fun todoWords(text: String): String = PRIORITY_RE.find(text)?.groupValues?.get(2) ?: text

/** Urgent, then high, then the rest. Stable, so the frame's oldest-first order holds inside each rank. */
fun sortTodo(rows: List<TodoRow>): List<TodoRow> = rows.sortedByDescending { todoPriority(it.text).rank }

/** Whole days since `at`; null when `at` does not parse or is in the future. */
fun todoAgeDays(at: String, today: LocalDate): Long? {
  val day = try {
    LocalDate.parse(at)
  } catch (_: DateTimeParseException) {
    return null
  }
  val days = ChronoUnit.DAYS.between(day, today)
  return days.takeIf { it >= 0 }
}

/** `3d ago` after the first day; nothing on the day it was written (the stamp's rule). */
fun ageLabel(at: String, today: LocalDate): String? {
  val days = todoAgeDays(at, today) ?: return null
  return if (days >= 1) "${days}d ago" else null
}

/** `#412 · dentist · 3d ago` — id, slug, age after the first day. */
fun todoMeta(row: TodoRow, today: LocalDate): String {
  val parts = mutableListOf("#${row.id}", row.slug)
  ageLabel(row.at, today)?.let { parts.add(it) }
  return parts.joinToString(" · ")
}

/** The checkbox: the one kernel write, as a visible turn. */
fun todoDoneCommand(id: Long): String = "/todo done $id"

/** Adding is plain words to Kit, who names the row. Null when there are no words. */
fun todoAddText(words: String): String? {
  val w = collapseWhitespace(words)
  return if (w.isEmpty()) null else "add to my list: $w"
}

/** The `/todo` footer's words past ten open; null under. */
fun pocketFooter(open: Int): String? =
  if (open > TODO_POCKET_MAX) "$open open — a pocket list; prune, or use a tracker" else null

/** Header chip text; the number is tasks that changed since the last open. */
fun tasksLabel(changed: Int): String = if (changed > 0) "tasks ($changed)" else "tasks"

/**
 * Keyed by **slug**: a rewrite changes the id, not the task. Stored on the
 * device (`todoSeen`, same shape as `aimsSeen`).
 */
fun seenTodo(rows: List<TodoRow>): Map<String, String> =
  rows.associate { it.slug to todoRowJson(it) }

fun changedTodo(rows: List<TodoRow>, seen: Map<String, String>): Int = changedRows(seenTodo(rows), seen)

fun encodeSeenTodo(seen: Map<String, String>): String = encodeSeenRows(seen)

fun parseSeenTodo(raw: String?): Map<String, String> = parseSeenRows(raw) { SLUG_RE.matches(it) }

internal fun todoRowJson(row: TodoRow): String =
  JSONArray().put(row.id).put(row.text).put(row.at).toString()

/**
 * A tick is local until the next `todo` frame settles it: a row that is gone
 * was done; one still there was not (the kernel said so) and un-ticks. Either
 * way the new list is the truth, so nothing stays ticked across a frame.
 */
fun settleTicked(): Set<Long> = emptySet()

/** Tap once; a second tap on a ticked row must not send `/todo done` again. */
fun canTick(id: Long, ticked: Set<Long>): Boolean = id !in ticked
