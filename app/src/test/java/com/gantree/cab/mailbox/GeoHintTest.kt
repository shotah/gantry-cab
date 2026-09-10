package com.gantree.cab.mailbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeoHintTest {
  @Test
  fun composeHintMatchesPendant() {
    assertEquals("GPS off", geoHint(false, null))
    assertEquals("GPS omitted (denied or unavailable)", geoHint(true, null))
    assertEquals("pin ±12m this send", geoHint(true, Geo(lat = 1.0, lon = 2.0, accuracyM = 12.4)))
    assertEquals("pin ±0m this send", geoHint(true, Geo(lat = 1.0, lon = 2.0)))
  }

  @Test
  fun textSendDoesNotShoutOmittedGps() {
    assertEquals(null, sendGeoHint(true, null))
    assertEquals(null, sendGeoHint(false, Geo(1.0, 2.0, 3.0)))
    assertEquals("pin ±3m this send", sendGeoHint(true, Geo(1.0, 2.0, 3.0)))
  }

  @Test
  fun geoFromFixDropsOutOfRangeHeadingAndSpeed() {
    val ok = geoFromFix(1.0, 2.0, 3.0, altM = 10.0, heading = 359.9, speedMps = 0.0)
    assertEquals(10.0, ok.altM)
    assertEquals(359.9, ok.heading)
    assertEquals(0.0, ok.speedMps)
    val drop = geoFromFix(1.0, 2.0, heading = 360.0, speedMps = -0.1)
    assertNull(drop.heading)
    assertNull(drop.speedMps)
    assertNull(geoFromFix(1.0, 2.0, accuracyM = -1.0).accuracyM)
  }
}
