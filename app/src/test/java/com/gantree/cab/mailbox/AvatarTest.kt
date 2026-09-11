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

  @Test
  fun backdropAndThemeUrlsMatchTheWorker() {
    assertEquals(
      "https://example.workers.dev/api/backdrop?slug=kit",
      backdropUrl("https://example.workers.dev/", "kit"),
    )
    assertEquals(
      "https://example.workers.dev/api/backdrop?slug=kit&v=9",
      backdropUrl("https://example.workers.dev/", "kit", 9),
    )
    assertEquals(
      "https://example.workers.dev/api/theme?slug=kit",
      themeUrl("https://example.workers.dev/", "kit"),
    )
  }

  @Test
  fun backdropRevAllowsZeroAndDropsJunk() {
    assertEquals(0, backdropRev("backdrop", 0))
    assertEquals(9, backdropRev("backdrop", 9))
    assertEquals(null, backdropRev("backdrop", -1))
    assertEquals(null, backdropRev("backdrop", 1.5))
    assertEquals(null, backdropRev("face", 9))
    assertEquals(null, backdropRev("backdrop", null))
  }

  @Test
  fun roomThemeNoticeClearsOnNullAndIgnoresJunk() {
    assertEquals("lamp", roomThemeNotice("theme", true, false, "lamp"))
    assertEquals("", roomThemeNotice("theme", true, true, "lamp"))
    assertEquals("", roomThemeNotice("theme", false, false, null))
    assertEquals(null, roomThemeNotice("theme", true, false, "nope"))
    assertEquals(null, roomThemeNotice("face", true, false, "lamp"))
  }
}
