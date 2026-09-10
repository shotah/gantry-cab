package com.gantree.cab.drive

import androidx.car.app.connection.CarConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotifyGateTest {
  @Test
  fun phoneThreadVisibleSkipsTheHeadsUp() {
    assertFalse(shouldPost(resumed = true, carAttached = false, kind = "reply"))
    assertFalse(shouldPost(resumed = true, carAttached = false, kind = "push"))
  }

  @Test
  fun headUnitGetsTheMouthUnlessCabIsOpen() {
    assertTrue(shouldPost(resumed = true, carAttached = true, kind = "reply"))
    assertTrue(shouldPost(resumed = false, carAttached = true, kind = "push"))
    assertTrue(shouldPost(resumed = false, carAttached = false, kind = "reply"))
    assertFalse(shouldPost(resumed = false, carAttached = true, kind = "reply", threadVisible = true))
  }

  @Test
  fun silentKindsNeverPost() {
    for (kind in listOf("draft", "typing", "ack", "error", "inbound", null)) {
      assertFalse(shouldPost(resumed = false, carAttached = true, kind = kind))
    }
  }

  @Test
  fun visiblePushBuzzesOnlyOnThePhone() {
    assertTrue(shouldBuzz(resumed = true, carAttached = false, kind = "push"))
    assertFalse(shouldBuzz(resumed = true, carAttached = false, kind = "reply"))
    assertFalse(shouldBuzz(resumed = true, carAttached = true, kind = "push"))
    assertFalse(shouldBuzz(resumed = false, carAttached = false, kind = "push"))
  }

  @Test
  fun projectionAndNativeCountAsAttached() {
    assertFalse(carConnectionAttached(null))
    assertFalse(carConnectionAttached(CarConnection.CONNECTION_TYPE_NOT_CONNECTED))
    assertTrue(carConnectionAttached(CarConnection.CONNECTION_TYPE_PROJECTION))
    assertTrue(carConnectionAttached(CarConnection.CONNECTION_TYPE_NATIVE))
    assertEquals(40L, PUSH_BUZZ_MS)
  }
}
