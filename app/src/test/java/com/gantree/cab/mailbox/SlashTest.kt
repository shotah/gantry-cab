package com.gantree.cab.mailbox

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SlashTest {
  private val catalog = listOf(
    SlashCommand("new", "reset this session"),
    SlashCommand("tools", "prefixed tool catalog"),
    SlashCommand("toolstats", "per-tool call ledger"),
    SlashCommand("tokens", "prompt token breakdown"),
    SlashCommand("brief", "hold a prefix ~6h", args = true),
  )

  @Test
  fun treatsALeadingSlashAsADraftUntilASpace() {
    assertNull(slashToken(""))
    assertNull(slashToken("hello"))
    assertEquals("", slashToken("/"))
    assertEquals("to", slashToken("/to"))
    assertEquals("new", slashToken("/NEW"))
    assertNull(slashToken("/new "))
    assertNull(slashToken("/brief google"))
  }

  @Test
  fun filtersCatalogAndInsertsATrailingSpaceForArgs() {
    assertEquals(emptyList<SlashCommand>(), matchSlash("hello", catalog))
    assertEquals(catalog.map { it.name }, matchSlash("/", catalog).map { it.name })
    assertEquals(listOf("tools", "toolstats", "tokens"), matchSlash("/to", catalog).map { it.name })
    assertEquals(listOf("new"), matchSlash("/new", catalog).map { it.name })
    assertEquals(emptyList<SlashCommand>(), matchSlash("/xyz", catalog))
    assertEquals("/brief ", slashInsert(catalog.first { it.name == "brief" }))
    assertEquals("/new", slashInsert(catalog.first { it.name == "new" }))
  }

  @Test
  fun parseCommandsDropsJunk() {
    assertEquals(emptyList<SlashCommand>(), parseCommands(null))
    val raw = JSONArray()
      .put(JSONObject().put("name", "NEW").put("hint", "reset this session").put("args", true))
      .put(JSONObject().put("name", "no spaces").put("hint", "nope"))
      .put(JSONObject().put("name", "x").put("hint", "ab"))
      .put(JSONObject().put("name", "/new").put("hint", "slash in name"))
      .put("nope")
      .put(JSONObject().put("name", "long").put("hint", "x".repeat(COMMAND_HINT_MAX + 1)))
    assertEquals(
      listOf(SlashCommand("new", "reset this session", args = true)),
      parseCommands(raw),
    )
    val many = JSONArray()
    for (i in 0 until COMMANDS_MAX + 4) {
      many.put(JSONObject().put("name", "c$i").put("hint", "hint for command $i"))
    }
    assertEquals(COMMANDS_MAX, parseCommands(many).size)
  }
}
