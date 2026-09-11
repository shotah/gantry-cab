package com.gantree.cab.mailbox

import org.junit.Assert.assertEquals
import org.junit.Test

class SendErrorTest {
  @Test
  fun wireTokensBecomeThePendantSentences() {
    assertEquals("Not sent — too much too fast. Wait a minute, then try again.", describeSendError("rate"))
    assertEquals("Not sent — too big for the room.", describeSendError(" Too Large "))
    assertEquals("Not sent.", describeSendError("bad frame"))
    assertEquals("Not sent.", describeSendError(null))
  }

  @Test
  fun localPhotoFailuresSaySo() {
    assertEquals("Photo not sent — still too big after shrinking.", describePhotoError("too large"))
    assertEquals("Photo not sent — couldn't read that image.", describePhotoError("bad photo"))
    assertEquals("too large", photoErrorToken("image too large"))
    assertEquals("bad photo", photoErrorToken("could not read that image"))
    assertEquals("bad photo", photoErrorToken(null))
  }
}
