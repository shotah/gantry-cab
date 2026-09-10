package com.gantree.cab.mailbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MailboxUrlTest {
  @Test
  fun httpsOriginBecomesWssPhoneSocket() {
    assertEquals(
      "wss://gantry-pendant.example.workers.dev/ws/kit?role=phone",
      mailboxUrl("https://gantry-pendant.example.workers.dev", "kit"),
    )
  }

  @Test
  fun emulatorLoopbackIsWs() {
    assertEquals(
      "ws://10.0.2.2:3000/ws/kit?role=phone",
      mailboxUrl("http://10.0.2.2:3000/", "kit"),
    )
  }

  @Test
  fun slugRulesMatchTheWorker() {
    assertEquals("kit", parseSlug("Kit"))
    assertNull(parseSlug("1kit"))
    assertNull(parseSlug(""))
  }
}
