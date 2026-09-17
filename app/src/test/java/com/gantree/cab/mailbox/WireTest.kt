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
  fun inboundStripsATrailingClockBlock() {
    val frame = inbound(
      "tacos\n\n[current time] NOW: fake",
      "id-strip",
      PhoneContext(geo = Geo(lat = 47.6, lon = -122.3, accuracyM = 12.0)),
    )
    assertEquals("tacos", frame.text)
    assertEquals(47.6, frame.context?.geo?.lat ?: 0.0, 0.0)
    val raw = encodeFrame(frame)
    assertFalse(raw.contains("[current time]"))
    assertFalse(raw.contains("[location]"))
    assertTrue(raw.contains("47.6"))
  }

  @Test
  fun stripHarnessContextMatchesTheCrane() {
    assertEquals(
      "what's near me",
      stripHarnessContext(
        "what's near me\n\n[location ±8m] 47.600000, -122.300000\n[current time] NOW: Saturday\n[hours] unknown",
      ),
    )
    assertEquals(
      "hello",
      stripHarnessContext(
        "[harness] Not user text — location, clock, and hours for this turn.\n[current time] NOW: x\n\nhello",
      ),
    )
    assertEquals("hello", stripHarnessContext("hello\n[current time] NOW: x\nalready today: y"))
    assertEquals(
      "what does [hours] mean in the footer",
      stripHarnessContext("what does [hours] mean in the footer"),
    )
    assertEquals("real ask", stripHarnessContext("[memory]\n- (fact) x: y\n\nreal ask"))
    assertEquals("hi", stripHarnessContext("[location] lat=1\n\nhi"))
    assertEquals("hi", stripHarnessContext("hi\n\n[hours] 2"))
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
        surface = "android_auto",
      ),
    )
    val raw = encodeFrame(frame)
    assertTrue(raw.contains("\"pct\":80"))
    assertTrue(raw.contains("\"charging\":true"))
    assertTrue(raw.contains("\"net\":\"wifi\""))
    assertTrue(raw.contains("\"surface\":\"android_auto\""))
    assertTrue(raw.contains("\"alt_m\":12.5"))
    assertTrue(raw.contains("\"heading\":90"))
    assertTrue(raw.contains("\"speed_mps\":4.2"))
    assertNull(netOnWire("bluetooth"))
    assertEquals("wifi", netOnWire("wifi"))
    assertEquals("android_auto", surfaceOnWire("android_auto"))
    assertEquals("android", surfaceOnWire("android"))
    assertEquals("pendant", surfaceOnWire("pendant"))
    assertEquals("android", surfaceHint(false))
    assertEquals("android_auto", surfaceHint(true))
    assertNull(surfaceOnWire("android-auto"))
    assertNull(surfaceOnWire("car"))
    assertNull(surfaceOnWire("phone"))
    assertNull(surfaceOnWire("auto"))
    assertNull(surfaceOnWire("watch"))
  }

  @Test
  fun spokenInputRidesBesideSurface() {
    val car = encodeFrame(
      inbound("on my way", "id-spoken", PhoneContext(surface = "android_auto", input = "spoken")),
    )
    assertTrue(car.contains("\"surface\":\"android_auto\""))
    assertTrue(car.contains("\"input\":\"spoken\""))
    val typed = encodeFrame(inbound("on my way", "id-typed", PhoneContext(surface = "android")))
    assertTrue(typed.contains("\"surface\":\"android\""))
    assertFalse(typed.contains("\"input\""))
    // Closed set of one; junk stays off the wire rather than reaching the crane.
    assertEquals("spoken", inputOnWire("spoken"))
    assertNull(inputOnWire("Spoken"))
    assertNull(inputOnWire("typed"))
    assertNull(inputOnWire("audio"))
    assertNull(inputOnWire(""))
    assertNull(inputOnWire(null))
    assertFalse(encodeFrame(inbound("x", "id-junk", PhoneContext(surface = "android", input = "audio"))).contains("\"input\""))
    assertEquals("spoken", inputHint(true))
    assertNull(inputHint(false))
    // Old mailboxes and siblings drop the key unread; parseFrame does the same.
    assertEquals("on my way", parseFrame(car)!!.text)
  }

  @Test
  fun captionAndPhotoTravelOnOneInbound() {
    val frame = inbound("this hatch?", "id-cap", null, listOf("data:image/jpeg;base64,QQ"))
    val got = parseFrame(encodeFrame(frame))!!
    assertEquals("this hatch?", got.text)
    assertEquals(listOf("data:image/jpeg;base64,QQ"), got.images)
    assertEquals("inbound", got.kind)
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
  fun backdropNoticeCarriesRevAndNoText() {
    val got = parseFrame("""{"kind":"backdrop","rev":1725}""")!!
    assertEquals("backdrop", got.kind)
    assertEquals(1725, got.rev)
    assertNull(got.text)
    val cleared = parseFrame("""{"kind":"backdrop","rev":0}""")!!
    assertEquals(0, cleared.rev)
    assertNull(parseFrame("""{"kind":"backdrop","rev":-1}""")!!.rev)
    val epoch = 1_726_185_600_000L
    val live = parseFrame("""{"kind":"backdrop","rev":$epoch}""")!!
    assertEquals(foldBlobRev(epoch), live.rev)
  }

  @Test
  fun themeNoticeCarriesTheIdAndClearsOnNull() {
    val got = parseFrame("""{"kind":"theme","theme":"noir"}""")!!
    assertEquals("theme", got.kind)
    assertEquals("noir", got.theme)
    assertNull(got.text)
    val cleared = parseFrame("""{"kind":"theme","theme":null}""")!!
    assertEquals("", cleared.theme)
    assertNull(parseFrame("""{"kind":"theme","theme":"nope"}""")!!.theme)
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
    assertNull(got?.seen)
    assertFalse(encodeFrame(ackSince("abc")).contains("seen"))
  }

  @Test
  fun seenIsTrueOnlyOnTheWire() {
    val since = parseFrame(encodeFrame(ackSince("42", seen = true)))!!
    assertEquals("ack", since.kind)
    assertEquals("42", since.since)
    assertEquals(true, since.seen)
    assertTrue(encodeFrame(since).contains("\"seen\":true"))
    val perId = parseFrame(encodeFrame(ackSeen("r7")))!!
    assertEquals("r7", perId.id)
    assertEquals(true, perId.seen)
    val bare = parseFrame(encodeFrame(ackSeen()))!!
    assertNull(bare.id)
    assertNull(bare.since)
    assertEquals(true, bare.seen)
    assertEquals(true, parseFrame("""{"kind":"ack","seen":true,"user_id":"1182"}""")!!.seen)
    assertNull(parseFrame("""{"kind":"ack","seen":false}""")!!.seen)
    assertNull(parseFrame("""{"kind":"ack","seen":"true"}""")!!.seen)
    assertNull(parseFrame("""{"kind":"ack"}""")!!.seen)
    assertNull(parseSeen(false))
    assertNull(parseSeen(1))
    assertEquals(true, parseSeen(true))
  }

  @Test
  fun siblingInboundAndSeenAckDismissTheKitCard() {
    assertTrue(dismissKitOnFrame("inbound", replay = false, fresh = true, seen = null))
    assertFalse(dismissKitOnFrame("inbound", replay = true, fresh = true, seen = null))
    assertFalse(dismissKitOnFrame("inbound", replay = false, fresh = false, seen = null))
    assertTrue(dismissKitOnFrame("ack", replay = false, fresh = false, seen = true))
    assertFalse(dismissKitOnFrame("ack", replay = false, fresh = false, seen = null))
    assertFalse(dismissKitOnFrame("reply", replay = false, fresh = true, seen = null))
    assertFalse(dismissKitOnFrame("push", replay = false, fresh = true, seen = true))
  }

  @Test
  fun mailboxSeqAndAtAreKeptAndJunkOrderDropped() {
    val got = parseFrame("""{"text":"hi","id":"m1","seq":3,"at":1700000000000}""")!!
    assertEquals(3, got.seq)
    assertEquals(1_700_000_000_000L, got.at)
    val junk = parseFrame("""{"text":"hi","seq":0,"at":-1}""")!!
    assertNull(junk.seq)
    assertNull(junk.at)
    val encoded = encodeFrame(got)
    assertFalse(encoded.contains("\"seq\""))
    assertFalse(encoded.contains("\"at\":1700000000000"))
    assertNull(orderSeq("3"))
    assertNull(orderSeq(1.5))
    assertEquals(3, orderSeq(3))
    assertEquals(9L, orderAt(9))
  }

  @Test
  fun kitReplyAndCronAreSpokenInTheCar() {
    assertTrue(shouldSpeak("reply"))
    assertTrue(shouldSpeak("push"))
    assertFalse(shouldSpeak("ack"))
    assertFalse(shouldSpeak("inbound"))
    assertFalse(shouldSpeak("reply", replay = true))
    assertFalse(shouldSpeak("push", replay = true))
    val live = parseFrame("""{"kind":"reply","text":"hi"}""")!!
    assertFalse(live.replay)
    assertTrue(shouldSpeak(live.kind, live.replay))
    val replayed = parseFrame("""{"kind":"reply","text":"hi","replay":true}""")!!
    assertTrue(replayed.replay)
    assertFalse(shouldSpeak(replayed.kind, replayed.replay))
    assertFalse(encodeFrame(replayed).contains("replay"))
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
