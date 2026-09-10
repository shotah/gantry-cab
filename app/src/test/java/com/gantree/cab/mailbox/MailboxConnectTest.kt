package com.gantree.cab.mailbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MailboxConnectTest {
  @Test
  fun connectNeedsASlugAndABearer() {
    assertEquals("Talking to needs a crane slug like kit.", mailboxConnectError("Kit!", "tok"))
    assertTrue(mailboxConnectError("kit", "")!!.contains("Google session"))
    assertNull(mailboxConnectError("kit", "jwe"))
  }

  @Test
  fun socketHintCallsOutRoomListOnForbidden() {
    val hint = mailboxSocketHint(403, "forbidden")
    assertTrue(hint.contains("HTTP 403"))
    assertTrue(hint.contains("room list"))
  }

  @Test
  fun socketHintKeepsTransportDetailWhenThereIsNoHttp() {
    assertEquals(
      "Mailbox socket down — failed to connect",
      mailboxSocketHint(null, "failed to connect"),
    )
  }

  @Test
  fun signedInWithNoCranesIsNotLive() {
    val hint = mailboxSignedInHint("ada@example.com", emptyList())
    assertTrue(hint.contains("ada@example.com"))
    assertTrue(hint.contains("no cranes"))
    assertEquals("Signed in as ada@example.com", mailboxSignedInHint("ada@example.com", listOf("kit")))
  }

  @Test
  fun allowlistCopyIsEmailThenSub() {
    assertEquals("ada@example.com\n1182", allowlistCopy("ada@example.com", "1182"))
    assertEquals("1182", allowlistCopy("  ", "1182"))
    assertEquals("", allowlistCopy("", ""))
  }

  @Test
  fun timeoutHintIsASentence() {
    assertTrue(mailboxTimeoutHint().contains("timed out"))
  }

  @Test
  fun retryStopsOnAuthAndNotFound() {
    assertFalse(mailboxShouldRetry(401))
    assertFalse(mailboxShouldRetry(403))
    assertFalse(mailboxShouldRetry(404))
    assertTrue(mailboxShouldRetry(null))
    assertTrue(mailboxShouldRetry(500))
  }

  @Test
  fun retryDelayDoublesThenCaps() {
    assertEquals(2_000L, mailboxRetryDelayMs(0))
    assertEquals(4_000L, mailboxRetryDelayMs(1))
    assertEquals(32_000L, mailboxRetryDelayMs(4))
    assertEquals(60_000L, mailboxRetryDelayMs(5))
    assertEquals(60_000L, mailboxRetryDelayMs(99))
  }

  @Test
  fun expiredSessionIsAConnectError() {
    assertTrue(mailboxConnectError("kit", "", sessionExpired = true)!!.contains("expired"))
    assertNull(mailboxConnectError("kit", "jwe", sessionExpired = false))
  }

  @Test
  fun sessionExpZeroIsNotExpired() {
    assertFalse(sessionExpired(0L, 9_999_999L))
    assertFalse(sessionExpired(100L, 99L))
    assertTrue(sessionExpired(100L, 100L))
    assertTrue(sessionExpired(100L, 101L))
  }

  @Test
  fun liveBearerDoesNotFallBackToSpikeAfterGoogleExpires() {
    assertEquals("jwe", liveBearer("jwe", 200L, "secret", 50L))
    assertEquals("", liveBearer("jwe", 100L, "secret", 100L))
    assertEquals("secret", liveBearer("", 100L, "secret", 200L))
  }

  @Test
  fun persistSpikeOnlyOnLoopbackDebug() {
    assertTrue(persistSpikeAllowed("http://10.0.2.2:3000", false, true))
    assertTrue(persistSpikeAllowed("http://localhost:3000", false, true))
    assertFalse(persistSpikeAllowed("https://pendant.example.com", false, true))
    assertTrue(persistSpikeAllowed("https://pendant.example.com", false, false))
    assertFalse(persistSpikeAllowed("http://10.0.2.2:3000", true, true))
    assertFalse(loopbackMailboxHost(""))
  }
}
