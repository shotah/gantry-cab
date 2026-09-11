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
  fun carCheckSaysWhetherThePhoneSeesTheHeadUnit() {
    val attached = carCheckText(carAttached = true)
    val loose = carCheckText(carAttached = false)
    assertTrue(attached.startsWith("Car check"))
    assertTrue(loose.startsWith("Car check"))
    assertTrue(attached.contains("Android Auto is attached"))
    assertTrue(loose.contains("does not see Android Auto"))
    assertTrue(attached != loose)
  }

  @Test
  fun bootAndUpdateRelistenOnlyWhenSignedIn() {
    assertTrue(bootShouldListen("android.intent.action.BOOT_COMPLETED", signedIn = true))
    assertTrue(bootShouldListen("android.intent.action.MY_PACKAGE_REPLACED", signedIn = true))
    assertFalse(bootShouldListen("android.intent.action.BOOT_COMPLETED", signedIn = false))
    assertFalse(bootShouldListen("android.intent.action.MY_PACKAGE_REPLACED", signedIn = false))
    assertFalse(bootShouldListen("android.intent.action.SCREEN_ON", signedIn = true))
    assertFalse(bootShouldListen(null, signedIn = true))
  }

  @Test
  fun carTestIsBlockedWhenAndroidWouldDropTheCard() {
    // IMPORTANCE_NONE 0, MIN 1, LOW 2, DEFAULT 3, HIGH 4 — Auto only heads-up (and reads) HIGH.
    assertTrue(carTestBlocked(notificationsEnabled = false, channelImportance = 4))
    assertTrue(carTestBlocked(notificationsEnabled = true, channelImportance = 0))
    assertTrue(carTestBlocked(notificationsEnabled = true, channelImportance = 2))
    assertTrue(carTestBlocked(notificationsEnabled = true, channelImportance = 3))
    assertFalse(carTestBlocked(notificationsEnabled = true, channelImportance = 4))
    assertFalse(carTestBlocked(notificationsEnabled = true, channelImportance = 5))
    assertFalse(carTestBlocked(notificationsEnabled = true, channelImportance = null))
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
