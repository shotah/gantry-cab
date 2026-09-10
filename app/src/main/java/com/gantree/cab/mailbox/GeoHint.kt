package com.gantree.cab.mailbox

import kotlin.math.roundToInt

fun geoHint(enabled: Boolean, geo: Geo?): String {
  if (!enabled) {
    return "GPS off"
  }
  if (geo != null) {
    val m = geo.accuracyM?.roundToInt() ?: 0
    return "pin ±${m}m this send"
  }
  return "GPS omitted (denied or unavailable)"
}
