package com.gantree.cab.drive

import org.junit.Assert.assertEquals
import org.junit.Test

class CabNotifierTest {
  @Test
  fun conversationIdIsStableForTheSlug() {
    assertEquals("cab-kit", CabNotifier.conversationId("kit"))
  }
}
