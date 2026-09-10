package com.gantree.cab.mailbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WireTest {
  @Test
  fun inboundRoundTripKeepsTextKindAndGeo() {
    val frame = inbound(
      "near me",
      "id-1",
      PhoneContext(
        at = "2026-09-09T12:00:00.000Z",
        tz = "America/Los_Angeles",
        geo = Geo(lat = 47.6, lon = -122.3, accuracyM = 12.0),
      ),
    )
    val raw = encodeFrame(frame)
    val got = parseFrame(raw)!!
    assertEquals("near me", got.text)
    assertEquals("inbound", got.kind)
    assertEquals("id-1", got.id)
    assertTrue(raw.contains("47.6"))
    assertFalse(raw.contains("[location]"))
  }

  @Test
  fun photoOnlyInboundKeepsTheDataUrl() {
    val frame = inbound("", "id-2", null, listOf("data:image/jpeg;base64,QQ"))
    val got = parseFrame(encodeFrame(frame))!!
    assertEquals(listOf("data:image/jpeg;base64,QQ"), got.images)
    assertEquals("inbound", got.kind)
    assertNull(got.text)
  }

  @Test
  fun faceFrameIsKeptForTheRev() {
    val got = parseFrame("""{"kind":"face","text":"9"}""")
    assertEquals("face", got?.kind)
    assertEquals("9", got?.text)
  }

  @Test
  fun pinEncodesGeoWithoutText() {
    val raw = encodeFrame(pinFrame(PhoneContext(geo = Geo(1.0, 2.0, 3.0))))
    assertTrue(raw.contains("\"kind\":\"pin\""))
    assertTrue(raw.contains("1"))
    assertFalse(raw.contains("\"text\""))
  }

  @Test
  fun cmdsFrameCarriesTheCatalog() {
    val raw = org.json.JSONObject()
      .put("kind", "cmds")
      .put(
        "commands",
        org.json.JSONArray().put(org.json.JSONObject().put("name", "new").put("hint", "reset this session")),
      )
      .toString()
    val got = parseFrame(raw)!!
    assertEquals("cmds", got.kind)
    assertEquals("new", got.commands?.first()?.name)
  }

  @Test
  fun ackSinceIsTheReconnectCatchUp() {
    val got = parseFrame(encodeFrame(ackSince("abc")))
    assertEquals("ack", got?.kind)
    assertEquals("abc", got?.since)
  }

  @Test
  fun kitReplyAndCronAreSpokenInTheCar() {
    assertTrue(shouldSpeak("reply"))
    assertTrue(shouldSpeak("push"))
    assertFalse(shouldSpeak("ack"))
    assertFalse(shouldSpeak("inbound"))
  }

  @Test
  fun junkAndKeepaliveAreDropped() {
    assertNull(parseFrame("nope"))
    assertNull(parseFrame("ping"))
    assertNull(parseFrame("pong"))
  }

  @Test
  fun inboundOmitsEmptyTextAndKeepsPhotos() {
    val frame = inbound("", "id-2", null, listOf("data:image/jpeg;base64,aa", "https://x/b.jpg"))
    val raw = encodeFrame(frame)
    val got = parseFrame(raw)!!
    assertNull(got.text)
    assertEquals(listOf("data:image/jpeg;base64,aa", "https://x/b.jpg"), got.images)
  }

  @Test
  fun emptyImagesAndEmptyContextStayOffTheWire() {
    val raw = encodeFrame(inbound("hi", "id-3", PhoneContext(), emptyList()))
    assertFalse(raw.contains("images"))
    assertFalse(raw.contains("context"))
  }

  @Test
  fun pinFrameCarriesGeoWithoutAccuracy() {
    val raw = encodeFrame(pinFrame(PhoneContext(geo = Geo(lat = 1.0, lon = 2.0))))
    val got = parseFrame(raw)!!
    assertEquals("pin", got.kind)
    assertTrue(raw.contains("\"lat\":1"))
    assertFalse(raw.contains("accuracy_m"))
  }

  @Test
  fun cmdsCatalogIsParsedAndJunkDropped() {
    val got = parseFrame(
      """{"kind":"cmds","commands":[{"name":"NEW","hint":"reset this session","args":true},{"name":"nope"}]}""",
    )!!
    assertEquals("cmds", got.kind)
    assertEquals(listOf(SlashCommand("new", "reset this session", args = true)), got.commands)
  }

  @Test
  fun blankImageUrlsAndEmptyStringsAreDropped() {
    val got = parseFrame("""{"text":"","kind":"","images":[{},{"url":""},{"url":"https://a"}]}""")!!
    assertNull(got.text)
    assertNull(got.kind)
    assertEquals(listOf("https://a"), got.images)
  }
}
