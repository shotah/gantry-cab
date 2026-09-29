package com.gantree.cab.mailbox

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pendant `docs/frontends.md` Aims board sample, word for word. */
private const val BOARD = """
{
  "kind": "aims",
  "aims": [
    {
      "area": "training",
      "sentence": "gym 3 mornings/wk",
      "rating30": 1.4,
      "sum7": 6,
      "streak": 2,
      "note": "asked",
      "note_at": "2026-09-25",
      "days": [
        { "day": "2026-09-22", "score": 2, "events": [411] },
        { "day": "2026-09-23", "score": -1, "events": [413] },
        { "day": "2026-09-24", "score": 0, "events": [] },
        { "day": "2026-09-25", "score": 3, "events": [415] },
        { "day": "2026-09-26", "score": 0, "events": [416] }
      ],
      "weeks": [
        { "start": "2026-09-13", "mean": 0.9, "up": 3, "against": 1, "metrics": [] },
        { "start": "2026-09-20", "mean": 1.4, "up": 4, "against": 1,
          "metrics": [{ "metric": "weight", "mean": 191.4, "unit": "lb", "n": 3 }] }
      ],
      "slope": 0.3,
      "block": { "days": 10, "up": 4, "against": 2, "mean": 0.4, "pct": 0.4 },
      "effect": { "a": "training", "b": "", "metric": "weight", "r": -0.42, "n": 9 }
    },
    { "area": "weight", "sentence": "under 190", "rating30": -0.5, "sum7": -2, "streak": 0, "note": "", "days": [] }
  ],
  "links": [
    { "a": "training", "b": "weight", "r": 0.38, "n": 12 }
  ]
}
"""

class AimsTest {
  @Test
  fun parsesTheFrontendsSample() {
    val board = parseAims(JSONObject(BOARD))!!
    assertEquals(2, board.aims.size)
    val t = board.aims[0]
    assertEquals("training", t.area)
    assertEquals("gym 3 mornings/wk", t.sentence)
    assertEquals(1.4, t.rating30, 0.0)
    assertEquals(6, t.sum7)
    assertEquals(2, t.streak)
    assertEquals("asked", t.note)
    assertEquals("2026-09-25", t.noteAt)
    assertEquals(5, t.days.size)
    assertEquals(listOf(411L), t.days[0].events)
    assertTrue(t.days[2].events.isEmpty())
    assertEquals(2, t.weeks.size)
    assertEquals("weight", t.weeks[1].metrics[0].metric)
    assertEquals(0.3, t.slope!!, 0.0)
    assertEquals(10, t.block!!.days)
    assertEquals(-0.42, t.effect!!.r, 0.0)
    assertEquals(1, board.links.size)
    assertEquals("training", board.links[0].a)
  }

  @Test
  fun emptyAimsIsARealClearAndMissingArrayIsJunk() {
    val clear = parseAims(JSONObject("""{"kind":"aims","aims":[]}"""))!!
    assertTrue(clear.isEmpty)
    assertNull(parseAims(JSONObject("""{"kind":"aims"}""")))
    assertNull(parseAims(JSONObject("""{"kind":"aims","aims":"nope"}""")))
  }

  @Test
  fun capsRowsDaysWeeksAndLinks() {
    val aims = (1..7).joinToString(",") { i ->
      """{"area":"a$i","sentence":"s","rating30":0,"sum7":0,"streak":0,"note":"","days":[]}"""
    }
    val board = parseAims(JSONObject("""{"aims":[$aims]}"""))!!
    assertEquals(AIMS_MAX, board.aims.size)
    assertEquals("a1", board.aims.first().area)

    val days = (1..20).joinToString(",") { i -> """{"day":"2026-09-${"%02d".format(i)}","score":1,"events":[]}""" }
    val weeks = (1..15).joinToString(",") { i -> """{"start":"2026-01-${"%02d".format(i)}","mean":0.1,"up":1,"against":0}""" }
    val one = parseAims(
      JSONObject("""{"aims":[{"area":"x","sentence":"s","rating30":0,"sum7":0,"streak":0,"note":"","days":[$days],"weeks":[$weeks]}]}"""),
    )!!.aims[0]
    assertEquals(AIM_DAYS_MAX, one.days.size)
    assertEquals(AIM_WEEKS_MAX, one.weeks.size)

    val links = (1..5).joinToString(",") { i -> """{"a":"x","b":"y","r":0.$i,"n":10}""" }
    val two = parseAims(
      JSONObject(
        """{"aims":[
          {"area":"x","sentence":"s","rating30":0,"sum7":0,"streak":0,"note":"","days":[]},
          {"area":"y","sentence":"s","rating30":0,"sum7":0,"streak":0,"note":"","days":[]}
        ],"links":[$links]}""",
      ),
    )!!
    assertEquals(AIM_LINKS_MAX, two.links.size)
  }

  @Test
  fun dropsABadRowNotTheBoard() {
    val board = parseAims(
      JSONObject(
        """{"aims":[
          {"area":"Bad Area","sentence":"s","rating30":0,"sum7":0,"streak":0,"note":"","days":[]},
          {"area":"nosentence","sentence":"","rating30":0,"sum7":0,"streak":0,"note":"","days":[]},
          {"area":"hot","sentence":"s","rating30":9,"sum7":0,"streak":0,"note":"","days":[]},
          {"area":"note","sentence":"s","rating30":0,"sum7":0,"streak":0,"note":"yelled","days":[]},
          {"area":"nodays","sentence":"s","rating30":0,"sum7":0,"streak":0,"note":""},
          {"area":"ok","sentence":"fine","rating30":-3,"sum7":1,"streak":1,"note":"quiet","days":[]}
        ]}""",
      ),
    )!!
    assertEquals(listOf("hot", "note", "nodays", "ok"), board.aims.map { it.area })
    assertEquals(3.0, board.aims[0].rating30, 0.0)
    assertEquals("yelled", board.aims[1].note)
    assertTrue(board.aims[2].days.isEmpty())
  }

  /** Crane omitempty: an aim with no day grid yet has no `days` key. That row stays. */
  @Test
  fun missingDaysKeepsTheAimSoTheBoardIsNotJustTheLastOne() {
    val board = parseAims(
      JSONObject(
        """{"aims":[
          {"area":"sleep","sentence":"in bed by 11","rating30":0,"sum7":0,"streak":0,"note":""},
          {"area":"Training","sentence":"gym 3 mornings","sum7":1},
          {"area":"weight","sentence":"under 190","rating30":-0.5,"sum7":-2,"streak":0,"note":"quiet",
           "days":[{"day":"2026-09-26","score":1,"events":[7]}]}
        ]}""",
      ),
    )!!
    assertEquals(listOf("sleep", "training", "weight"), board.aims.map { it.area })
    assertEquals(0.0, board.aims[1].rating30, 0.0)
    assertTrue(board.aims[0].days.isEmpty())
    assertEquals(1, board.aims[2].days.size)
  }

  @Test
  fun halfFormedBlockEffectWeekAndLinkAreDroppedWhole() {
    val aim = parseAims(
      JSONObject(
        """{"aims":[{"area":"x","sentence":"s","rating30":0,"sum7":0,"streak":0,"note":"",
          "days":[{"day":"bad","score":1},{"day":"2026-09-01","score":"two"},{"day":"2026-09-02","score":1,"events":[0,-3,"x",7]}],
          "weeks":[{"start":"2026-09-01","mean":0.5},{"start":"2026-09-08","mean":0.5,"up":1,"against":0,"metrics":[{"metric":"","mean":1,"n":1},{"metric":"w","mean":1,"n":1}]}],
          "block":{"days":10,"up":4},
          "effect":{"a":"x","b":"","metric":"","r":0.5,"n":9},
          "slope":"fast"}],
          "links":[{"a":"x","b":"x","r":0.5,"n":9},{"a":"x","b":"zzz","r":0.5,"n":9},{"a":"x","b":"y","r":"nope","n":9}]}""",
      ),
    )!!.aims[0]
    assertEquals(1, aim.days.size)
    assertEquals(listOf(7L), aim.days[0].events)
    assertEquals(1, aim.weeks.size)
    assertEquals(1, aim.weeks[0].metrics.size)
    assertNull(aim.block)
    assertNull(aim.effect)
    assertNull(aim.slope)
  }

  @Test
  fun linksOnlyBetweenAreasOnThisBoard() {
    val board = parseAims(
      JSONObject(
        """{"aims":[{"area":"x","sentence":"s","rating30":0,"sum7":0,"streak":0,"note":"","days":[]}],
          "links":[{"a":"x","b":"y","r":0.5,"n":9}]}""",
      ),
    )!!
    assertTrue(board.links.isEmpty())
  }

  @Test
  fun stampAndTrendLinesMatchTheSlashOutput() {
    val board = parseAims(JSONObject(BOARD))!!
    val t = board.aims[0]
    assertEquals("30d +1.4 · 7d +6 · streak 2 · asked", statsLine(t))
    assertEquals("slope +0.3/wk · block 4/10 (40%) · weight r -0.42 (n 9)", trendLine(t))
    assertEquals("training → next-day weight r +0.38 (n 12)", linkLine(board.links[0]))
    val w = board.aims[1]
    assertEquals("30d -0.5 · 7d -2 · streak 0", statsLine(w))
    assertEquals("", trendLine(w))
  }

  @Test
  fun signedAndLabels() {
    assertEquals("+1.4", signed(1.4))
    assertEquals("-0.5", signed(-0.5))
    assertEquals("0", signed(0.0))
    assertEquals("+6", signed(6))
    assertEquals("-2", signed(-2))
    assertEquals("0", signed(0))
    assertEquals("goals (2)", goalsLabel(2))
    assertEquals("goals", goalsLabel(0))
    assertEquals("/aims training", askAim("training"))
    assertEquals("/aims", ASK_AIMS_REPORT)
    assertEquals("/aims rubric", ASK_AIMS_RUBRIC)
  }

  /** Pendant "The badge is a call to action, not the board size." */
  @Test
  fun badgeCountsChangesSinceTheLastOpenNotAims() {
    val board = parseAims(JSONObject(BOARD))!!
    // Never seen: the whole board counts.
    assertEquals(2, changedAims(board, emptyMap()))
    // Opened: nothing to tap for.
    val seen = seenAims(board)
    assertEquals(0, changedAims(board, seen))
    // Same board replayed on reconnect: still nothing.
    assertEquals(0, changedAims(parseAims(JSONObject(BOARD))!!, seen))
    // One row moved (streak 2 → 3): one.
    val moved = parseAims(JSONObject(BOARD.replace("\"streak\": 2", "\"streak\": 3")))!!
    assertEquals(1, changedAims(moved, seen))
    // An aim gone counts; an aim new counts.
    val onlyWeight = AimsBoard(aims = board.aims.filter { it.area == "weight" })
    assertEquals(1, changedAims(onlyWeight, seen))
    val plusOne = AimsBoard(aims = board.aims + board.aims[1].copy(area = "sleep"))
    assertEquals(1, changedAims(plusOne, seen))
    // Board cleared while two were seen: two gone (the button is hidden anyway).
    assertEquals(2, changedAims(AimsBoard(), seen))
  }

  @Test
  fun seenBoardRoundTripsAndJunkIsNeverSeen() {
    val board = parseAims(JSONObject(BOARD))!!
    val seen = seenAims(board)
    assertEquals(setOf("training", "weight"), seen.keys)
    assertEquals(seen, parseSeenAims(encodeSeenAims(seen)))
    assertTrue(parseSeenAims(null).isEmpty())
    assertTrue(parseSeenAims("").isEmpty())
    assertTrue(parseSeenAims("not json").isEmpty())
    assertTrue(parseSeenAims("[1,2]").isEmpty())
    // Only string rows under area-shaped keys survive.
    assertEquals(mapOf("ok" to "{}"), parseSeenAims("""{"ok":"{}","Bad Key":"{}","n":3}"""))
  }

  @Test
  fun rideOnTheWireOnlyAsAnAimsKind() {
    val frame = parseFrame(BOARD)!!
    assertEquals("aims", frame.kind)
    assertNotNull(frame.aims)
    assertNull(frame.text)
    val other = parseFrame("""{"kind":"push","text":"hi","aims":[]}""")!!
    assertNull(other.aims)
    assertFalse(movesCursor("aims"))
  }
}
