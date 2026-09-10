package com.gantree.cab.mailbox

data class EmojiEntry(
  val emoji: String,
  val name: String,
  val aliases: List<String> = emptyList(),
)

data class EmojiEdit(
  val text: String,
  val cursor: Int,
)

private data class Row(val emoji: String, val name: String, val aliases: String? = null)

private val ROWS = listOf(
  Row("😀", "grinning", "D"),
  Row("😃", "smiley"),
  Row("😄", "smile"),
  Row("😁", "grin"),
  Row("😆", "laughing", "xd"),
  Row("😅", "sweat_smile"),
  Row("🤣", "rofl"),
  Row("😂", "joy"),
  Row("🙂", "slightly_smiling"),
  Row("😊", "blush"),
  Row("😇", "innocent"),
  Row("😉", "wink"),
  Row("😍", "heart_eyes"),
  Row("😘", "kissing_heart", "kiss"),
  Row("😋", "yum"),
  Row("😛", "stuck_out_tongue", "P"),
  Row("😜", "stuck_out_tongue_winking_eye"),
  Row("🤪", "zany"),
  Row("😎", "sunglasses"),
  Row("🤓", "nerd"),
  Row("🧐", "monocle"),
  Row("🤔", "thinking"),
  Row("🤨", "raised_eyebrow"),
  Row("😐", "neutral"),
  Row("😑", "expressionless"),
  Row("🙄", "rolling_eyes"),
  Row("😏", "smirk"),
  Row("😒", "unamused"),
  Row("😞", "disappointed"),
  Row("😔", "pensive"),
  Row("😕", "confused"),
  Row("🙁", "slightly_frowning"),
  Row("☹️", "frowning"),
  Row("😣", "persevere"),
  Row("😖", "confounded"),
  Row("😫", "tired"),
  Row("😩", "weary"),
  Row("🥺", "pleading"),
  Row("😢", "cry"),
  Row("😭", "sob"),
  Row("😤", "triumph"),
  Row("😠", "angry"),
  Row("😡", "rage"),
  Row("🤬", "cursing"),
  Row("😳", "flushed"),
  Row("🥵", "hot"),
  Row("🥶", "cold"),
  Row("😱", "scream"),
  Row("😨", "fearful"),
  Row("😰", "cold_sweat"),
  Row("😥", "disappointed_relieved"),
  Row("😓", "sweat"),
  Row("🤗", "hugging"),
  Row("🤭", "hand_over_mouth"),
  Row("🤫", "shushing"),
  Row("🤥", "lying"),
  Row("😶", "no_mouth"),
  Row("🫠", "melting"),
  Row("😴", "sleeping", "zzz"),
  Row("🥱", "yawn"),
  Row("😷", "mask"),
  Row("🤒", "thermometer"),
  Row("🤕", "head_bandage"),
  Row("🤢", "nauseated"),
  Row("🤮", "vomiting"),
  Row("🤧", "sneezing"),
  Row("🥳", "partying"),
  Row("🥸", "disguised"),
  Row("🤡", "clown"),
  Row("👻", "ghost"),
  Row("💀", "skull"),
  Row("👽", "alien"),
  Row("🤖", "robot"),
  Row("💩", "poop", "hankey,shit"),
  Row("🙈", "see_no_evil"),
  Row("🙉", "hear_no_evil"),
  Row("🙊", "speak_no_evil"),
  Row("👋", "wave"),
  Row("🤚", "raised_back_of_hand"),
  Row("🖐️", "hand"),
  Row("✋", "raised_hand"),
  Row("🖖", "vulcan"),
  Row("👌", "ok_hand", "ok"),
  Row("🤌", "pinched_fingers"),
  Row("🤏", "pinching_hand"),
  Row("✌️", "v", "victory"),
  Row("🤞", "crossed_fingers"),
  Row("🫰", "hand_with_index_finger_and_thumb_crossed"),
  Row("🤟", "love_you_gesture"),
  Row("🤘", "metal"),
  Row("🤙", "call_me"),
  Row("👈", "point_left"),
  Row("👉", "point_right"),
  Row("👆", "point_up"),
  Row("👇", "point_down"),
  Row("☝️", "point_up_2"),
  Row("👍", "thumbsup", "+1,thumbs_up"),
  Row("👎", "thumbsdown", "-1,thumbs_down"),
  Row("✊", "fist"),
  Row("👊", "punch"),
  Row("🤛", "left_facing_fist"),
  Row("🤜", "right_facing_fist"),
  Row("👏", "clap"),
  Row("🙌", "raised_hands"),
  Row("🫶", "heart_hands"),
  Row("👐", "open_hands"),
  Row("🤲", "palms_up"),
  Row("🤝", "handshake"),
  Row("🙏", "pray", "thanks"),
  Row("✍️", "writing"),
  Row("💅", "nail_care"),
  Row("🤳", "selfie"),
  Row("💪", "muscle"),
  Row("🦾", "mechanical_arm"),
  Row("🦵", "leg"),
  Row("🦶", "foot"),
  Row("👂", "ear"),
  Row("👃", "nose"),
  Row("👀", "eyes"),
  Row("👁️", "eye"),
  Row("👅", "tongue"),
  Row("👄", "lips"),
  Row("🧠", "brain"),
  Row("🫀", "anatomical_heart"),
  Row("🦴", "bone"),
  Row("🤷", "shrug", "person_shrugging"),
  Row("🤦", "facepalm", "person_facepalming"),
  Row("❤️", "heart"),
  Row("🧡", "orange_heart"),
  Row("💛", "yellow_heart"),
  Row("💚", "green_heart"),
  Row("💙", "blue_heart"),
  Row("💜", "purple_heart"),
  Row("🖤", "black_heart"),
  Row("🤍", "white_heart"),
  Row("💔", "broken_heart"),
  Row("❣️", "heart_exclamation"),
  Row("💕", "two_hearts"),
  Row("💖", "sparkling_heart"),
  Row("💗", "heartpulse"),
  Row("💘", "cupid"),
  Row("💝", "gift_heart"),
  Row("💞", "revolving_hearts"),
  Row("💟", "heart_decoration"),
  Row("✨", "sparkles"),
  Row("⭐", "star"),
  Row("🌟", "star2"),
  Row("💫", "dizzy"),
  Row("🔥", "fire"),
  Row("💯", "100"),
  Row("💥", "boom", "collision"),
  Row("💢", "anger"),
  Row("💦", "sweat_drops"),
  Row("💨", "dash"),
  Row("🕳️", "hole"),
  Row("💬", "speech", "speech_balloon"),
  Row("👁️‍🗨️", "eye_speech"),
  Row("💭", "thought", "thought_balloon"),
  Row("💤", "zzz_symbol"),
  Row("🎉", "tada", "party"),
  Row("🎊", "confetti"),
  Row("🎈", "balloon"),
  Row("🎁", "gift"),
  Row("🏆", "trophy"),
  Row("🥇", "first_place", "medal"),
  Row("🥈", "second_place"),
  Row("🥉", "third_place"),
  Row("✅", "white_check_mark", "check"),
  Row("❌", "x", "cross"),
  Row("⚠️", "warning"),
  Row("❓", "question"),
  Row("❗", "exclamation"),
  Row("💡", "bulb"),
  Row("📌", "pushpin", "pin"),
  Row("📍", "round_pushpin"),
  Row("📝", "memo"),
  Row("🔔", "bell"),
  Row("🔑", "key"),
  Row("🔒", "lock"),
  Row("🔓", "unlock"),
  Row("🔗", "link"),
  Row("📎", "paperclip"),
  Row("📷", "camera"),
  Row("📸", "camera_flash"),
  Row("💻", "computer"),
  Row("📱", "phone", "iphone"),
  Row("📧", "email", "mail"),
  Row("📚", "books", "book"),
  Row("✏️", "pencil"),
  Row("✂️", "scissors"),
  Row("🔨", "hammer"),
  Row("🔧", "wrench"),
  Row("🗑️", "trash"),
  Row("🚀", "rocket"),
  Row("🚗", "car"),
  Row("✈️", "airplane"),
  Row("🏠", "house"),
  Row("🌍", "earth", "globe"),
  Row("☀️", "sunny", "sun"),
  Row("🌙", "moon"),
  Row("☁️", "cloud"),
  Row("🌈", "rainbow"),
  Row("❄️", "snowflake"),
  Row("💧", "droplet"),
  Row("⚡", "zap"),
  Row("☕", "coffee"),
  Row("🍺", "beer"),
  Row("🍕", "pizza"),
  Row("🍔", "hamburger", "burger"),
  Row("🍟", "fries"),
  Row("🌮", "taco"),
  Row("🍣", "sushi"),
  Row("🍪", "cookie"),
  Row("🎂", "birthday", "cake"),
  Row("🍰", "cake_slice"),
  Row("🍎", "apple"),
  Row("🌹", "rose"),
  Row("🌻", "sunflower"),
  Row("🌵", "cactus"),
  Row("🌳", "tree"),
  Row("🐶", "dog"),
  Row("🐱", "cat"),
  Row("🦊", "fox"),
  Row("🐻", "bear"),
  Row("🐼", "panda"),
  Row("🦄", "unicorn"),
  Row("🐝", "bee"),
  Row("🐢", "turtle"),
  Row("🐙", "octopus"),
  Row("🦋", "butterfly"),
  Row("🐞", "bug"),
)

private fun aliasesOf(raw: String?): List<String> {
  if (raw.isNullOrBlank()) {
    return emptyList()
  }
  return raw.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
}

val emojiCatalog: List<EmojiEntry> = ROWS.map { row ->
  EmojiEntry(emoji = row.emoji, name = row.name, aliases = aliasesOf(row.aliases))
}

private fun normName(name: String): String = name.lowercase().replace("-", "_")

private val byName: Map<String, String> = buildMap {
  for (entry in emojiCatalog) {
    put(normName(entry.name), entry.emoji)
    for (alias in entry.aliases) {
      put(normName(alias), entry.emoji)
    }
  }
}

fun emojiForShortcode(name: String): String? = byName[normName(name)]

private val DEFAULT_PICK = listOf(
  "grinning", "smile", "joy", "blush", "wink", "heart_eyes", "thinking", "sunglasses",
  "cry", "sob", "rage", "scream", "partying", "shrug", "facepalm", "thumbsup", "thumbsdown",
  "ok_hand", "clap", "pray", "wave", "muscle", "heart", "fire", "100", "tada", "sparkles",
  "star", "white_check_mark", "x", "warning", "eyes", "poop", "skull", "ghost", "robot",
  "dog", "cat", "rocket", "coffee", "pizza", "sunny", "moon", "zap",
)

fun searchEmoji(query: String): List<EmojiEntry> {
  val q = query.trim().lowercase().replace(":", "")
  if (q.isEmpty()) {
    return DEFAULT_PICK.mapNotNull { name -> emojiCatalog.find { it.name == name } }
  }
  val exact = query.trim()
  return emojiCatalog.filter { entry ->
    entry.name.contains(q) || entry.aliases.any { it.contains(q) } || entry.emoji == exact
  }
}

private data class Span(val start: Int, val end: Int, val value: String)

private data class Emoticon(val token: String, val emoji: String, val hold: Boolean)

private val SHORTCODE = Regex(":[a-z0-9_+-]{1,32}:", RegexOption.IGNORE_CASE)

private val EMOTICONS = listOf(
  Emoticon(":'(", "😢", false),
  Emoticon(":-D", "😀", true),
  Emoticon(":-P", "😛", true),
  Emoticon(":-O", "😮", true),
  Emoticon(":-)", "😊", false),
  Emoticon(":-(", "🙁", false),
  Emoticon(";-)", "😉", false),
  Emoticon(":D", "😀", true),
  Emoticon(":P", "😛", true),
  Emoticon(":O", "😮", true),
  Emoticon(":)", "😊", false),
  Emoticon(":(", "🙁", false),
  Emoticon(";)", "😉", false),
  Emoticon(":/", "😕", false),
  Emoticon(":|", "😐", false),
  Emoticon("xD", "😆", true),
)

private fun isBreakBefore(text: String, index: Int): Boolean {
  if (index == 0) {
    return true
  }
  val ch = text[index - 1]
  return ch.isWhitespace() || ch == '('
}

private fun isTerminator(ch: Char?, hold: Boolean, trailing: Boolean): Boolean {
  if (ch == null) {
    return trailing || !hold
  }
  return ch.isWhitespace() || ch == '.' || ch == ',' || ch == '!' || ch == '?' || ch == ';' || ch == ')'
}

private fun shortcodeSpans(text: String): List<Span> {
  val spans = mutableListOf<Span>()
  for (match in SHORTCODE.findAll(text)) {
    val raw = match.value
    val name = raw.substring(1, raw.length - 1)
    val emoji = emojiForShortcode(name) ?: continue
    spans.add(Span(match.range.first, match.range.last + 1, emoji))
  }
  return spans
}

private fun overlaps(spans: List<Span>, start: Int, end: Int): Boolean =
  spans.any { start < it.end && end > it.start }

private fun emoticonSpans(text: String, trailing: Boolean, blocked: List<Span>): List<Span> {
  val spans = mutableListOf<Span>()
  var i = 0
  while (i < text.length) {
    if (!isBreakBefore(text, i) || overlaps(blocked, i, i + 1)) {
      i += 1
      continue
    }
    var hit: Span? = null
    for (item in EMOTICONS) {
      val end = i + item.token.length
      if (end > text.length) {
        continue
      }
      val slice = text.substring(i, end)
      if (!slice.equals(item.token, ignoreCase = true)) {
        continue
      }
      if (overlaps(blocked, i, end)) {
        continue
      }
      if (!isTerminator(text.getOrNull(end), item.hold, trailing)) {
        continue
      }
      hit = Span(i, end, item.emoji)
      break
    }
    if (hit != null) {
      spans.add(hit)
      i = hit.end
      continue
    }
    i += 1
  }
  return spans
}

private fun applySpans(text: String, cursor: Int, spans: List<Span>): EmojiEdit {
  if (spans.isEmpty()) {
    return EmojiEdit(text, cursor)
  }
  val out = StringBuilder()
  var nextCursor = cursor
  var last = 0
  var parked = false
  for (span in spans) {
    out.append(text, last, span.start)
    val at = out.length
    out.append(span.value)
    if (!parked) {
      if (cursor >= span.end) {
        nextCursor += span.value.length - (span.end - span.start)
      } else if (cursor > span.start) {
        nextCursor = at + span.value.length
        parked = true
      }
    }
    last = span.end
  }
  out.append(text, last, text.length)
  val done = out.toString()
  return EmojiEdit(done, nextCursor.coerceIn(0, done.length))
}

fun applyEmoji(text: String, cursor: Int, whenTo: String): EmojiEdit {
  val safeCursor = cursor.coerceIn(0, text.length)
  val codes = shortcodeSpans(text)
  val faces = emoticonSpans(text, whenTo == "send", codes)
  val spans = (codes + faces).sortedBy { it.start }
  return applySpans(text, safeCursor, spans)
}
