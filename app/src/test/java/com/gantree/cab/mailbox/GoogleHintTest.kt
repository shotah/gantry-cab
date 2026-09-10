package com.gantree.cab.mailbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleHintTest {
  @Test
  fun cancelledIsPlain() {
    assertEquals(
      "Google sign-in cancelled.",
      googleSignInHint("androidx.credentials.exceptions.GetCredentialCancellationException", "cancelled"),
    )
  }

  @Test
  fun missingShaExplainsConsole() {
    val none = googleSignInHint(
      "androidx.credentials.exceptions.NoCredentialException",
      null,
    )
    assertTrue(none.contains("SHA-1"))
    val console = googleSignInHint(
      "java.lang.Exception",
      "During begin sign in, failure response from one tap: 16: [28444] Developer console is not set up correctly",
    )
    assertTrue(console.contains("Android OAuth"))
  }

  @Test
  fun otherMessagesPassThrough() {
    assertEquals("network down", googleSignInHint("java.io.IOException", "network down"))
  }
}
