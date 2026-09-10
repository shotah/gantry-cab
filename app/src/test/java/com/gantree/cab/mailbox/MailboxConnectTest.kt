package com.gantree.cab.mailbox

import org.junit.Assert.assertEquals
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
}
