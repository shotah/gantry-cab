package com.gantree.cab.mailbox

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoHintTest {
  @Test
  fun composeHintMatchesPendant() {
    assertEquals("GPS off", geoHint(false, null))
    assertEquals("GPS omitted (denied or unavailable)", geoHint(true, null))
    assertEquals("pin ±12m this send", geoHint(true, Geo(lat = 1.0, lon = 2.0, accuracyM = 12.4)))
    assertEquals("pin ±0m this send", geoHint(true, Geo(lat = 1.0, lon = 2.0)))
  }
}
