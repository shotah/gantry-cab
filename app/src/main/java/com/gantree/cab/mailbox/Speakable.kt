package com.gantree.cab.mailbox

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser

/**
 * Chirp 3 HD takes 5 000 bytes per `text:synthesize`. Leave room for
 * multibyte punctuation and the JSON around it. Pendant `SPEAK_BYTES_MAX`.
 */
const val SPEAK_BYTES_MAX = 4_000

private val SPEECH_FLAVOUR = GFMFlavourDescriptor()
private val SPACES = Regex("[ \t]+")
/** A soft or hard break inside a block is one boundary, not "newline space" or a blank line. */
private val NEWLINE_PAD = Regex(" ?\n+ ?")
private val SENTENCE_BREAK = Regex("(?<=[.!?])\\s+|\\n+")

/**
 * Markdown → words a TTS engine can say. Same parser the bubble paints with
 * (`ChatMarkdown`), different output: bold / italic / headings / links keep
 * their text, a fenced block becomes "code", an image becomes "photo"; tables,
 * rules and raw HTML are dropped; emoji are dropped. Blocks join on a newline so
 * the engine still hears a sentence boundary. Pendant `lib/phone/speakable.ts`.
 */
fun speakable(markdown: String): String {
  val src = markdown.trim()
  if (src.isEmpty()) {
    return ""
  }
  val root = MarkdownParser(SPEECH_FLAVOUR).buildMarkdownTreeFromString(src)
  val walk = SpeechWalk(src)
  walk.block(root)
  return walk.parts.joinToString("\n")
}

/**
 * Keep a long reply under the synth cap without cutting a sentence in half.
 * Falls back to a hard cut ([capUtf8]) when one sentence is itself over the cap.
 */
fun clipForSpeech(text: String, maxBytes: Int = SPEAK_BYTES_MAX): String {
  if (utf8Len(text) <= maxBytes) {
    return text
  }
  var out = ""
  for (sentence in text.split(SENTENCE_BREAK)) {
    val next = if (out.isEmpty()) sentence else "$out $sentence"
    if (utf8Len(next) > maxBytes) {
      break
    }
    out = next
  }
  if (out.isNotEmpty()) {
    return out
  }
  return capUtf8(text, maxBytes)
}

/** One block's text → what the engine hears: emoji gone, runs of spaces / tabs collapsed, trimmed. */
internal fun spokenWords(raw: String): String {
  val sb = StringBuilder(raw.length)
  var i = 0
  while (i < raw.length) {
    val cp = raw.codePointAt(i)
    if (!isEmojiPart(cp)) {
      sb.appendCodePoint(cp)
    }
    i += Character.charCount(cp)
  }
  return sb.toString().replace(SPACES, " ").replace(NEWLINE_PAD, "\n").trim()
}

/**
 * Pictographs plus the joiners and skin tones that ride with them — what the
 * pendant's `\p{Extended_Pictographic}|\p{Emoji_Modifier}|ZWJ|VS16` drops so a
 * reader does not say "red heart". Block ranges rather than a Unicode property:
 * `Character.isEmoji` is API 34 and Android's regex property names differ from
 * the JVM's. Regional indicators (flags) ride along; nobody wants those read.
 */
internal fun isEmojiPart(cp: Int): Boolean = when (cp) {
  0x200D, 0xFE0E, 0xFE0F, 0x20E3 -> true
  0x00A9, 0x00AE, 0x203C, 0x2049, 0x2122, 0x2139, 0x24C2, 0x25B6, 0x25C0 -> true
  0x2934, 0x2935, 0x3030, 0x303D, 0x3297, 0x3299 -> true
  in 0x2194..0x2199, in 0x21A9..0x21AA, in 0x25AA..0x25AB, in 0x25FB..0x25FE -> true
  in 0x2300..0x23FF, in 0x2600..0x27BF, in 0x2B00..0x2BFF -> true
  in 0x1F000..0x1FAFF, in 0x1FC00..0x1FFFD -> true
  else -> false
}

private fun utf8Len(s: String): Int = s.toByteArray(Charsets.UTF_8).size

/** strip-markdown's table, walked over the intellij tree: containers recurse, blocks flush, markers vanish. */
private class SpeechWalk(private val src: CharSequence) {
  val parts = mutableListOf<String>()
  private val line = StringBuilder()

  fun block(node: ASTNode) {
    when (node.type) {
      MarkdownElementTypes.MARKDOWN_FILE,
      MarkdownElementTypes.UNORDERED_LIST,
      MarkdownElementTypes.ORDERED_LIST,
      MarkdownElementTypes.LIST_ITEM,
      MarkdownElementTypes.BLOCK_QUOTE,
      GFMElementTypes.ALERT,
      -> {
        flush()
        for (child in node.children) {
          block(child)
        }
        flush()
      }
      MarkdownElementTypes.PARAGRAPH,
      MarkdownElementTypes.ATX_1,
      MarkdownElementTypes.ATX_2,
      MarkdownElementTypes.ATX_3,
      MarkdownElementTypes.ATX_4,
      MarkdownElementTypes.ATX_5,
      MarkdownElementTypes.ATX_6,
      MarkdownElementTypes.SETEXT_1,
      MarkdownElementTypes.SETEXT_2,
      -> {
        flush()
        inline(node)
        flush()
      }
      MarkdownElementTypes.CODE_FENCE, MarkdownElementTypes.CODE_BLOCK -> {
        flush()
        parts.add("code")
      }
      GFMElementTypes.TABLE,
      MarkdownElementTypes.HTML_BLOCK,
      MarkdownElementTypes.LINK_DEFINITION,
      MarkdownTokenTypes.HORIZONTAL_RULE,
      MarkdownTokenTypes.EOL,
      MarkdownTokenTypes.WHITE_SPACE,
      MarkdownTokenTypes.LIST_BULLET,
      MarkdownTokenTypes.LIST_NUMBER,
      MarkdownTokenTypes.BLOCK_QUOTE,
      -> {}
      // Bare inline content under a tight list item; the container flushes it as one line.
      else -> inline(node)
    }
  }

  private fun inline(node: ASTNode) {
    when (node.type) {
      MarkdownElementTypes.IMAGE -> line.append("photo")
      MarkdownElementTypes.CODE_SPAN -> children(node, MarkdownTokenTypes.BACKTICK)
      MarkdownElementTypes.INLINE_LINK,
      MarkdownElementTypes.FULL_REFERENCE_LINK,
      MarkdownElementTypes.SHORT_REFERENCE_LINK,
      -> {
        val label = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_TEXT }
          ?: node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_LABEL }
        if (label != null) {
          children(label, MarkdownTokenTypes.LBRACKET, MarkdownTokenTypes.RBRACKET)
        }
      }
      MarkdownElementTypes.AUTOLINK -> children(node, MarkdownTokenTypes.LT, MarkdownTokenTypes.GT)
      MarkdownElementTypes.EMPH, MarkdownElementTypes.STRONG -> children(node, MarkdownTokenTypes.EMPH)
      GFMElementTypes.STRIKETHROUGH -> children(node, GFMTokenTypes.TILDE)
      MarkdownTokenTypes.EOL, MarkdownTokenTypes.HARD_LINE_BREAK -> line.append('\n')
      MarkdownTokenTypes.HTML_TAG,
      MarkdownTokenTypes.ATX_HEADER,
      MarkdownTokenTypes.SETEXT_1,
      MarkdownTokenTypes.SETEXT_2,
      MarkdownTokenTypes.BLOCK_QUOTE,
      GFMTokenTypes.CHECK_BOX,
      -> {}
      else -> if (node.children.isEmpty()) line.append(node.getTextInNode(src)) else children(node)
    }
  }

  private fun children(node: ASTNode, vararg skip: org.intellij.markdown.IElementType) {
    for (child in node.children) {
      if (child.type !in skip) {
        inline(child)
      }
    }
  }

  private fun flush() {
    val words = spokenWords(line.toString())
    line.setLength(0)
    if (words.isNotEmpty()) {
      parts.add(words)
    }
  }
}
