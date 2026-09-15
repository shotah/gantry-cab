package com.gantree.cab.mailbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Same cases as pendant `test/phone/speakable.test.ts` so the two mouths read a reply the same way. */
class SpeakableTest {
  @Test
  fun dropsAsterisksUnderscoresAndHeadingMarksButKeepsTheWords() {
    assertEquals("Bold and soft and also soft", speakable("**Bold** and *soft* and _also soft_"))
    assertEquals("Tonight\nRain after 8.", speakable("## Tonight\nRain after 8."))
    assertEquals("not this this", speakable("~~not this~~ this"))
  }

  @Test
  fun readsInlineCodeAsItsTextAndAFencedBlockAsTheWordCode() {
    assertEquals("Run npm test first.", speakable("Run `npm test` first."))
    assertEquals(
      "Here you go:\ncode\nThen open it.",
      speakable("Here you go:\n\n```sh\nnpm run dev\n```\n\nThen open it."),
    )
  }

  @Test
  fun saysPhotoForAnImageAndOnlyTheLabelForALink() {
    assertEquals("photo", speakable("![the hatch](data:image/jpeg;base64,/9j/)"))
    assertEquals("See the map now", speakable("See [the map](https://maps.example/x) now"))
  }

  @Test
  fun flattensListsToOneLinePerItemAndDropsTablesRulesAndRawHtml() {
    assertEquals("milk\neggs\nfirst\nsecond", speakable("- milk\n- eggs\n\n1. first\n2. second"))
    assertEquals(
      "Before\nAfter",
      speakable("Before\n\n| a | b |\n| - | - |\n| 1 | 2 |\n\n---\n\n<br>\n\nAfter"),
    )
  }

  @Test
  fun dropsEmojiAndTheirJoinersSoTheReaderDoesNotNameThem() {
    assertEquals("On my way see you soon", speakable("On my way 🚗💨 see you soon ❤️"))
    assertEquals("done", speakable("👍🏽 done"))
    assertEquals("family night", speakable("👩‍👩‍👧 family night"))
  }

  @Test
  fun returnsEmptyForBlankOrMarkdownOnlyInput() {
    assertEquals("", speakable(""))
    assertEquals("", speakable("   \n"))
    assertEquals("", speakable("---"))
    assertEquals("code", speakable("```\nx\n```"))
  }

  @Test
  fun keepsPlainProseUntouchedApartFromWhitespace() {
    assertEquals("Sure. It's about ten minutes out.", speakable("Sure.  It's   about ten minutes out."))
  }

  @Test
  fun quotesAndSoftBreaksStayWordsWithABoundary() {
    assertEquals("Kit said\nno.", speakable("> Kit said\n> no."))
    assertEquals("one\ntwo", speakable("one  \ntwo"))
  }

  @Test
  fun emojiPartsCoverPictographsModifiersAndJoinersOnly() {
    assertTrue(isEmojiPart(0x1F697))
    assertTrue(isEmojiPart(0x1F3FD))
    assertTrue(isEmojiPart(0x200D))
    assertTrue(isEmojiPart(0xFE0F))
    assertTrue(isEmojiPart(0x2764))
    assertFalse(isEmojiPart('a'.code))
    assertFalse(isEmojiPart('é'.code))
    assertFalse(isEmojiPart('.'.code))
    assertFalse(isEmojiPart(0x4E2D))
  }

  @Test
  fun clipReturnsShortTextAsIs() {
    assertEquals("Hi there.", clipForSpeech("Hi there."))
  }

  @Test
  fun clipCutsOnASentenceBoundaryUnderTheCap() {
    val sentence = "This is a sentence that goes on for a bit. "
    val long = sentence.repeat(200)
    val clipped = clipForSpeech(long, 500)
    assertTrue(clipped.toByteArray(Charsets.UTF_8).size <= 500)
    assertTrue(clipped.endsWith("bit."))
    assertTrue(clipped.length > 400)
  }

  @Test
  fun clipHardCutsASingleOversizeSentenceWithoutSplittingACodePoint() {
    val run = "é".repeat(3000)
    val clipped = clipForSpeech(run, 101)
    assertTrue(clipped.toByteArray(Charsets.UTF_8).size <= 101)
    assertFalse(clipped.contains('\uFFFD'))
    assertEquals(50, clipped.length)
  }

  @Test
  fun clipDefaultsToTheChirpBudget() {
    val long = "word ".repeat(2000)
    assertTrue(clipForSpeech(long).toByteArray(Charsets.UTF_8).size <= SPEAK_BYTES_MAX)
  }
}
