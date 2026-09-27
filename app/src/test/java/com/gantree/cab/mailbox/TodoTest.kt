package com.gantree.cab.mailbox

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** The pendant `docs/frontends.md` Tasks board sample. */
private const val LIST = """
{
  "kind": "todo",
  "todo": [
    { "id": 412, "slug": "dentist",  "text": "call to book a cleaning", "at": "2026-09-23" },
    { "id": 418, "slug": "passport", "text": "renew, by Oct 15",        "at": "2026-09-26" }
  ]
}
"""

class TodoTest {
  private val today = LocalDate.parse("2026-09-26")

  @Test
  fun parsesTheFrontendsSampleOldestFirst() {
    val rows = parseTodo(JSONObject(LIST))!!
    assertEquals(listOf(412L, 418L), rows.map { it.id })
    assertEquals(listOf("dentist", "passport"), rows.map { it.slug })
    assertEquals("call to book a cleaning", rows[0].text)
    assertEquals("2026-09-23", rows[0].at)
  }

  @Test
  fun emptyIsAClearAndAMissingArrayIsJunk() {
    assertEquals(emptyList<TodoRow>(), parseTodo(JSONObject("""{"kind":"todo","todo":[]}""")))
    assertNull(parseTodo(JSONObject("""{"kind":"todo"}""")))
    assertNull(parseTodo(JSONObject("""{"kind":"todo","todo":"nope"}""")))
  }

  @Test
  fun dropsABadRowNotTheListAndARepeatOfEitherKey() {
    val rows = parseTodo(
      JSONObject(
        """{"todo":[
          {"id":0,"slug":"zero","text":"no","at":"2026-09-01"},
          {"id":1,"slug":"Bad","text":"no","at":"2026-09-01"},
          {"id":2,"slug":"blank","text":"   ","at":"2026-09-01"},
          {"id":3,"slug":"nodate","text":"x","at":"yesterday"},
          {"id":4,"slug":"ok","text":"  call   them  ","at":"2026-09-01"},
          {"id":4,"slug":"again","text":"same id","at":"2026-09-02"},
          {"id":5,"slug":"ok","text":"same slug","at":"2026-09-02"}
        ]}""",
      ),
    )!!
    assertEquals(listOf(4L), rows.map { it.id })
    assertEquals("call them", rows[0].text)
  }

  @Test
  fun keepsAHundredAndDropsTheRest() {
    val rows = (1..120).joinToString(",") { i ->
      """{"id":$i,"slug":"t$i","text":"do $i","at":"2026-09-01"}"""
    }
    val parsed = parseTodo(JSONObject("""{"todo":[$rows]}"""))!!
    assertEquals(TODO_MAX, parsed.size)
    assertEquals(1L, parsed.first().id)
    assertEquals(100L, parsed.last().id)
  }

  @Test
  fun textCollapsesAndCapsAt240Runes() {
    val long = "あ".repeat(TODO_TEXT_MAX + 5)
    val rows = parseTodo(
      JSONObject("""{"todo":[{"id":1,"slug":"x","text":"$long","at":"2026-09-01"}]}"""),
    )!!
    assertEquals(TODO_TEXT_MAX, rows[0].text.codePointCount(0, rows[0].text.length))
  }

  @Test
  fun ageShowsAfterTheFirstDay() {
    val old = TodoRow(412, "dentist", "call", "2026-09-23")
    val fresh = TodoRow(418, "passport", "renew", "2026-09-26")
    assertEquals("#412 · dentist · 3d ago", todoMeta(old, today))
    assertEquals("#418 · passport", todoMeta(fresh, today))
    assertNull(ageLabel("not-a-day", today))
    assertNull(ageLabel("2026-09-27", today))
  }

  @Test
  fun theCheckboxIsOneCommandAndAddIsPlainWords() {
    assertEquals("/todo done 412", todoDoneCommand(412))
    assertEquals("add to my list: book a cleaning", todoAddText("  book   a cleaning "))
    assertNull(todoAddText("   "))
    assertEquals("/todo", TODO_LIST_COMMAND)
    assertTrue(canTick(412, emptySet()))
    assertFalse(canTick(412, setOf(412L)))
    assertTrue(settleTicked().isEmpty())
  }

  @Test
  fun footerOnlyPastTen() {
    assertNull(pocketFooter(10))
    assertEquals("11 open — a pocket list; prune, or use a tracker", pocketFooter(11))
    assertEquals("tasks", tasksLabel(0))
    assertEquals("tasks (2)", tasksLabel(2))
  }

  /** A rewrite changes the id, not the task, so the badge keys on slug. */
  @Test
  fun badgeKeysOnSlugSoARewriteIsOneChange() {
    val rows = parseTodo(JSONObject(LIST))!!
    assertEquals(2, changedTodo(rows, emptyMap()))
    val seen = seenTodo(rows)
    assertEquals(0, changedTodo(rows, seen))
    // Same words, new id: the slug's row changed, so one.
    val rewritten = rows.map { if (it.slug == "dentist") it.copy(id = 500) else it }
    assertEquals(1, changedTodo(rewritten, seen))
    assertEquals(1, changedTodo(rows.filter { it.slug == "passport" }, seen))
    assertEquals(seen, parseSeenTodo(encodeSeenTodo(seen)))
    assertTrue(parseSeenTodo("nope").isEmpty())
  }

  @Test
  fun rideOnTheWireOnlyAsATodoKind() {
    val frame = parseFrame(LIST)!!
    assertEquals("todo", frame.kind)
    assertEquals(2, frame.todo!!.size)
    assertNull(frame.text)
    assertNull(parseFrame("""{"kind":"push","text":"hi","todo":[]}""")!!.todo)
    assertFalse(movesCursor("todo"))
  }
}
