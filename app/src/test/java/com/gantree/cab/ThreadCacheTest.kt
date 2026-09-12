package com.gantree.cab

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ThreadCacheTest {
  @get:Rule
  val tmp = TemporaryFolder()

  private val origin = "https://mailbox.example"

  private fun thread() = listOf(
    ChatLine("a1", true, "hatch?", "inbound", pending = true, at = 10L, seq = 1),
    ChatLine("k1", false, "latched", "reply", at = 20L, seq = 2),
    ChatLine("p1", false, "", "push", photo = "data:image/jpeg;base64,aa", at = 30L, seq = 3),
    ChatLine("a2", true, "nope", "inbound", at = 40L, failed = "Not sent — too big for the room."),
    ChatLine(DRAFT_ID, false, "Gate's on…", "draft", at = 50L),
  )

  @Test
  fun writeThenReadRoundTripsEveryFieldButTheDraft() {
    val cache = ThreadCache(File(tmp.root, "recall/thread.json"))
    assertEquals(emptyList<ChatLine>(), cache.read(origin, "kit"))
    cache.write(origin, "kit", thread())
    val back = cache.read(origin, "kit")
    assertEquals(thread().dropLast(1), back)
    assertTrue(back[0].pending)
    assertEquals(1, back[0].seq)
    assertNull(back[3].seq)
    assertEquals("Not sent — too big for the room.", back[3].failed)
    assertFalse(File(tmp.root, "recall/thread.json.tmp").exists())
  }

  @Test
  fun anotherRoomOrOriginReadsBackEmpty() {
    val cache = ThreadCache(File(tmp.root, "thread.json"))
    cache.write(origin, "kit", thread())
    assertEquals(emptyList<ChatLine>(), cache.read(origin, "ada"))
    assertEquals(emptyList<ChatLine>(), cache.read("https://other.example", "kit"))
    assertEquals(4, cache.read(origin, "kit").size)
  }

  @Test
  fun junkOnDiskIsAnEmptyThread() {
    val file = File(tmp.root, "thread.json")
    file.writeText("{nope")
    assertEquals(emptyList<ChatLine>(), ThreadCache(file).read(origin, "kit"))
    file.writeText("""{"origin":"$origin","slug":"kit","lines":[{"text":"no id"},7,{"id":"ok","text":"hi"}]}""")
    assertEquals(listOf(ChatLine("ok", false, "hi", null)), ThreadCache(file).read(origin, "kit"))
  }

  @Test
  fun unwritablePathIsQuiet() {
    val blocked = File(tmp.root, "not-a-dir")
    blocked.writeText("x")
    val cache = ThreadCache(File(blocked, "thread.json"))
    cache.write(origin, "kit", thread())
    assertEquals(emptyList<ChatLine>(), cache.read(origin, "kit"))
  }

  @Test
  fun capKeepsTheNewestLinesAndOneMaxPhotoFits() {
    // Each line is 171 chars of JSON: two fit in 400, three do not.
    val big = (0 until 6).map { ChatLine("b$it", false, "x".repeat(100), "reply", at = it.toLong()) }
    val raw = encodeThread(origin, "kit", big, maxChars = 400)
    val ids = decodeThread(raw, origin, "kit").map { it.id }
    assertEquals(listOf("b4", "b5"), ids)
    val one = decodeThread(encodeThread(origin, "kit", big, maxChars = 1), origin, "kit")
    assertEquals(emptyList<ChatLine>(), one)
    val photo = ChatLine("ph", true, "", "inbound", photo = "data:image/jpeg;base64," + "a".repeat(2_000_000))
    assertEquals(listOf("ph"), decodeThread(encodeThread(origin, "kit", listOf(photo)), origin, "kit").map { it.id })
  }

  @Test
  fun onlyWirePhotoShapesComeBack() {
    val lines = listOf(
      ChatLine("f", true, "", "inbound", photo = "file:///etc/passwd"),
      ChatLine("h", true, "", "inbound", photo = "https://img.example/a.jpg"),
    )
    val back = decodeThread(encodeThread(origin, "kit", lines), origin, "kit")
    assertNull(back[0].photo)
    assertEquals("https://img.example/a.jpg", back[1].photo)
  }

  @Test
  fun decodeCapsAtTheThreadMaxAndDropsNonPositiveSeq() {
    val many = (0 until 100).map { ChatLine("m$it", false, "n$it", "reply", at = it.toLong()) }
    val back = decodeThread(encodeThread(origin, "kit", many), origin, "kit")
    assertEquals(80, back.size)
    assertEquals("m20", back.first().id)
    val o = JSONObject(encodeThread(origin, "kit", listOf(ChatLine("z", false, "z", "reply", seq = 0))))
    assertNull(decodeThread(o.toString(), origin, "kit").single().seq)
  }
}
