package com.gantree.cab.mailbox

import org.json.JSONArray

const val COMMANDS_MAX = 32
const val COMMAND_NAME_MAX = 32
const val COMMAND_HINT_MAX = 256

private val NAME_RE = Regex("^[a-z][a-z0-9_]{0,31}$")

data class SlashCommand(
  val name: String,
  val hint: String,
  val args: Boolean = false,
)

fun slashToken(text: String): String? {
  if (!text.startsWith("/") || text.any { it.isWhitespace() }) {
    return null
  }
  return text.substring(1).lowercase()
}

fun matchSlash(text: String, catalog: List<SlashCommand>): List<SlashCommand> {
  val token = slashToken(text) ?: return emptyList()
  return catalog.filter { it.name.startsWith(token) }
}

fun slashInsert(cmd: SlashCommand): String =
  if (cmd.args) "/${cmd.name} " else "/${cmd.name}"

fun parseCommands(raw: JSONArray?): List<SlashCommand> {
  if (raw == null) {
    return emptyList()
  }
  val out = mutableListOf<SlashCommand>()
  for (i in 0 until raw.length()) {
    if (out.size >= COMMANDS_MAX) {
      break
    }
    val item = raw.optJSONObject(i) ?: continue
    val name = item.optString("name").trim().lowercase()
    val hint = item.optString("hint").trim()
    if (!NAME_RE.matches(name) || name.length > COMMAND_NAME_MAX) {
      continue
    }
    if (hint.length < 3 || hint.length > COMMAND_HINT_MAX) {
      continue
    }
    out.add(SlashCommand(name = name, hint = hint, args = item.optBoolean("args", false)))
  }
  return out
}
