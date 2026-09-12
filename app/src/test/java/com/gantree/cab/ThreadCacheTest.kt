package com.gantree.cab

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ThreadCacheTest {
  @get:Rule
  val tmp = TemporaryFolder()

  private val room = ThreadRoom("https://mailbox.example", "kit", "ada@example.com")

  private fun thread() = listOf(
    ChatLine("a0", true, "still sending", "inbound", pending = true, at = 5L),
    ChatLine("a1", true, "hatch?", "inbound", at = 10L, seq = 1),
    ChatLine("k1", false, "latched", "reply", at = 20L, seq = 2),
    ChatLine("p1", false, "", "push", photo = "data:image/jpeg;base64,aa", at = 30L, seq = 3),
    ChatLine("a2", true, "nope", "inbound", at = 40L, failed = "Not sent — too big for the room."),
    ChatLine(DRAFT_ID, false, "Gate's on…", "draft", at = 50L),
  )

  /** Settled bubbles only — what pendant `persistableThread` keeps. */
  private fun settled() = thread().drop(1).dropLast(1)

  @Test
  fun writeThenReadRoundTripsSettledBubblesOnly() {
    val cache = ThreadCache(File(tmp.root, "recall/thread.json"))
    assertEquals(emptyList<ChatLine>(), cache.read(room))
    cache.write(room, thread())
    val back = cache.read(room)
    assertEquals(settled(), back)
    assertEquals(1, back[0].seq)
    assertNull(back[3].seq)
    assertEquals("Not sent — too big for the room.", back[3].failed)
    assertFalse(back.any { it.pending })
    assertFalse(File(tmp.root, "recall/thread.json.tmp").exists())
  }

  @Test
  fun persistableThreadDropsSendingAndDrafts() {
    assertEquals(listOf("a1", "k1", "p1", "a2"), persistableThread(thread()).map { it.id })
  }

  @Test
  fun persistableThreadStripsLive() {
    val live = ChatLine("r1", false, "Hello", "reply", live = true)
    val kept = persistableThread(listOf(live)).single()
    assertEquals("r1", kept.id)
    assertEquals(false, kept.live)
  }

  @Test
  fun anotherRoomOriginOrHumanReadsBackEmpty() {
    val cache = ThreadCache(File(tmp.root, "thread.json"))
    cache.write(room, thread())
    assertEquals(emptyList<ChatLine>(), cache.read(room.copy(slug = "ada")))
    assertEquals(emptyList<ChatLine>(), cache.read(room.copy(origin = "https://other.example")))
    assertEquals(emptyList<ChatLine>(), cache.read(room.copy(user = "bob@example.com")))
    assertEquals(emptyList<ChatLine>(), cache.read(room.copy(user = "")))
    assertEquals(4, cache.read(room).size)
  }

  @Test
  fun junkOnDiskIsAnEmptyThread() {
    val file = File(tmp.root, "thread.json")
    file.writeText("{nope")
    assertEquals(emptyList<ChatLine>(), ThreadCache(file).read(room))
    file.writeText(
      """{"origin":"${room.origin}","slug":"kit","user":"${room.user}","lines":[{"text":"no id"},7,{"id":"ok","text":"hi"}]}""",
    )
    assertEquals(listOf(ChatLine("ok", false, "hi", null)), ThreadCache(file).read(room))
  }

  @Test
  fun unwritablePathIsQuiet() {
    val blocked = File(tmp.root, "not-a-dir")
    blocked.writeText("x")
    val cache = ThreadCache(File(blocked, "thread.json"))
    cache.write(room, thread())
    assertEquals(emptyList<ChatLine>(), cache.read(room))
  }

  @Test
  fun capKeepsTheNewestLinesAndOneMaxPhotoFits() {
    // Each line is 155 chars of JSON: two fit in 400, three do not.
    val big = (0 until 6).map { ChatLine("b$it", false, "x".repeat(100), "reply", at = it.toLong()) }
    val raw = encodeThread(room, big, maxChars = 400)
    assertEquals(listOf("b4", "b5"), decodeThread(raw, room).map { it.id })
    assertEquals(emptyList<ChatLine>(), decodeThread(encodeThread(room, big, maxChars = 1), room))
    val photo = ChatLine("ph", true, "", "inbound", photo = "data:image/jpeg;base64," + "a".repeat(2_000_000))
    assertEquals(listOf("ph"), decodeThread(encodeThread(room, listOf(photo)), room).map { it.id })
  }

  @Test
  fun onlyWirePhotoShapesComeBack() {
    val lines = listOf(
      ChatLine("f", true, "", "inbound", photo = "file:///etc/passwd"),
      ChatLine("h", true, "", "inbound", photo = "https://img.example/a.jpg"),
    )
    val back = decodeThread(encodeThread(room, lines), room)
    assertNull(back[0].photo)
    assertEquals("https://img.example/a.jpg", back[1].photo)
  }

  @Test
  fun decodeCapsAtTheThreadMaxAndDropsNonPositiveSeq() {
    val many = (0 until 100).map { ChatLine("m$it", false, "n$it", "reply", at = it.toLong()) }
    val back = decodeThread(encodeThread(room, many), room)
    assertEquals(80, back.size)
    assertEquals("m20", back.first().id)
    val o = JSONObject(encodeThread(room, listOf(ChatLine("z", false, "z", "reply", seq = 0))))
    assertNull(decodeThread(o.toString(), room).single().seq)
  }
}
