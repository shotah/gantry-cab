package com.gantree.cab.mailbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AvatarTest {
  @Test
  fun displaySlugTitleCases() {
    assertEquals("Kit", displaySlug(""))
    assertEquals("Kit", displaySlug("  "))
    assertEquals("Kit", displaySlug("kit"))
    assertEquals("Ada", displaySlug("ada"))
  }

  @Test
  fun faceRevOnlyFromFaceKind() {
    assertEquals(9, faceRev("face", "9"))
    assertNull(faceRev("reply", "9"))
    assertNull(faceRev("face", "nope"))
    assertNull(faceRev("face", "0"))
    assertNull(faceRev("face", "-1"))
    assertNull(faceRev("face", null))
  }

  @Test
  fun avatarUrlPinsARev() {
    assertEquals(
      "https://example.workers.dev/api/avatar?slug=kit",
      avatarUrl("https://example.workers.dev/", "kit"),
    )
    assertEquals(
      "https://example.workers.dev/api/avatar?slug=kit&v=3",
      avatarUrl("https://example.workers.dev/", "kit", 3),
    )
  }
}
