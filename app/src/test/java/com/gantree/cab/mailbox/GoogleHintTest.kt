package com.gantree.cab.mailbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleHintTest {
  @Test
  fun cancelledKeepsTheRawException() {
    val hint = googleSignInHint(
      "androidx.credentials.exceptions.GetCredentialCancellationException",
      "cancelled",
    )
    assertTrue(hint.contains("SHA-1"))
    assertTrue(hint.contains("GetCredentialCancellationException"))
    assertTrue(hint.contains("cancelled"))
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
    assertTrue(console.contains("28444"))
  }

  @Test
  fun otherMessagesPassThroughWithClass() {
    val hint = googleSignInHint("java.io.IOException", "network down")
    assertTrue(hint.contains("network down"))
    assertTrue(hint.contains("IOException"))
  }

  @Test
  fun throwableWalksTheCause() {
    val err = RuntimeException("outer", IllegalStateException("inner boom"))
    val hint = googleSignInHint(err)
    assertTrue(hint.contains("outer"))
    assertTrue(hint.contains("inner boom"))
  }
}
