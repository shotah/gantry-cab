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
  fun contextCarriesBatteryNetAndMotion() {
    val frame = inbound(
      "hi",
      "id-ctx",
      PhoneContext(
        at = "2026-09-10T12:00:00.000Z",
        tz = "UTC",
        geo = geoFromFix(47.6, -122.3, 12.0, altM = 12.5, heading = 90.0, speedMps = 4.2),
        battery = BatteryHint(80, true),
        net = "wifi",
      ),
    )
    val raw = encodeFrame(frame)
    assertTrue(raw.contains("\"pct\":80"))
    assertTrue(raw.contains("\"charging\":true"))
    assertTrue(raw.contains("\"net\":\"wifi\""))
    assertTrue(raw.contains("\"alt_m\":12.5"))
    assertTrue(raw.contains("\"heading\":90"))
    assertTrue(raw.contains("\"speed_mps\":4.2"))
    assertNull(netOnWire("bluetooth"))
    assertEquals("wifi", netOnWire("wifi"))
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

  @Test
  fun inboundTextIsCapped() {
    val t = "x".repeat(TEXT_CHARS_MAX + 40)
    val got = parseFrame("""{"kind":"reply","text":"$t"}""")!!
    assertEquals(TEXT_CHARS_MAX, got.text!!.length)
  }

  @Test
  fun inboundTextIsCappedByUtf8Bytes() {
    val t = "é".repeat((TEXT_BYTES_MAX / 2) + 4)
    val got = capUtf8(t)
    assertTrue(got.toByteArray(Charsets.UTF_8).size <= TEXT_BYTES_MAX)
    assertTrue(got.length < t.length)
  }

  @Test
  fun notifyBodyPrefersTextThenPhotoThenPing() {
    assertEquals("hi", notifyBody(" hi ", true))
    assertEquals("Photo", notifyBody("  ", true))
    assertEquals("ping", notifyBody(null, false))
  }

  @Test
  fun batteryAndNetHintsMatchTheWire() {
    assertEquals(BatteryHint(1, false), batteryHint(1, false))
    assertNull(batteryHint(-1, false))
    assertNull(batteryHint(101, true))
    assertEquals("wifi", netHint(true, true))
    assertEquals("cellular", netHint(false, true))
    assertEquals("unknown", netHint(false, false))
  }

  @Test
  fun inboundImagesRejectOversizeDataAndLongHttp() {
    val huge = "data:image/jpeg;base64," + "A".repeat(2_100_000)
    assertFalse(acceptInboundImage(huge))
    val longHttp = "https://x/" + "a".repeat(3_000)
    assertFalse(acceptInboundImage(longHttp))
    assertTrue(acceptInboundImage("https://example.test/a.jpg"))
    assertFalse(acceptInboundImage("http://example.test/a.jpg"))
  }
}
