package com.gantree.cab.mailbox

import org.junit.Assert.assertEquals
import org.junit.Test

/** Mirrors pendant `test/phone/lang.test.ts`: same ids, labels, and recognizer tags. */
class LangTest {
  @Test
  fun catalogIsThePendantsInTheSameOrder() {
    assertEquals(listOf("en", "ja", "zh", "vi"), LANG_IDS)
    assertEquals("en", DEFAULT_LANG)
    assertEquals(
      listOf("English", "日本語 · Japanese", "中文 · Mandarin", "Tiếng Việt · Vietnamese"),
      LANGUAGES.map { it.label },
    )
  }

  @Test
  fun junkOrMissingIsEnglish() {
    assertEquals("zh", parseLang("zh"))
    assertEquals("ja", parseLang("ja"))
    assertEquals("vi", parseLang("vi"))
    assertEquals("en", parseLang("klingon"))
    assertEquals("en", parseLang("ja-JP"))
    assertEquals("en", parseLang("vi-VN"))
    assertEquals("en", parseLang("JA"))
    assertEquals("en", parseLang(""))
    assertEquals("en", parseLang(null))
  }

  @Test
  fun recognizerTagsAreBcp47AndMandarinIsZhCn() {
    assertEquals("en-US", speechLang("en"))
    assertEquals("ja-JP", speechLang("ja"))
    assertEquals("zh-CN", speechLang("zh"))
    assertEquals("vi-VN", speechLang("vi"))
    assertEquals("en-US", speechLang("nope"))
  }

  @Test
  fun labelsFollowTheId() {
    assertEquals("日本語 · Japanese", langLabel("ja"))
    assertEquals("Tiếng Việt · Vietnamese", langLabel("vi"))
    assertEquals("English", langLabel("nope"))
  }
}
