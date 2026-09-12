package com.gantree.cab.mailbox

/** Mirror ai-gantry `stripHarnessContext` / pendant `lib/phone/text.ts`. */
private val HARNESS_PREFIXES = listOf(
  "[harness]",
  "[location",
  "[current time]",
  "[hours]",
  "[memory]",
)

private fun harnessTagLine(line: String): Boolean {
  val t = line.trim()
  return HARNESS_PREFIXES.any { t.startsWith(it) }
}

private fun harnessBlock(part: String): Boolean {
  for (line in part.split("\n")) {
    val t = line.trim()
    if (t.isEmpty()) {
      continue
    }
    return harnessTagLine(t)
  }
  return false
}

private fun stripTrailingHarnessLines(part: String): String {
  val lines = part.split("\n")
  for (i in lines.indices) {
    if (!harnessTagLine(lines[i])) {
      continue
    }
    if (i == 0) {
      return part
    }
    return lines.take(i).joinToString("\n")
  }
  return part
}

/** Drop pasted / old-client clock and hydration blocks so they are not stored as speech. */
fun stripHarnessContext(raw: String): String {
  val s = raw.trim()
  if (s.isEmpty()) {
    return ""
  }
  val kept = mutableListOf<String>()
  for (part in s.split("\n\n")) {
    if (harnessBlock(part)) {
      continue
    }
    val trimmed = stripTrailingHarnessLines(part).trimEnd('\n')
    if (trimmed.isBlank()) {
      continue
    }
    kept.add(trimmed)
  }
  return kept.joinToString("\n\n").trim()
}
