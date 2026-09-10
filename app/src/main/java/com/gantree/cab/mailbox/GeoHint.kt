package com.gantree.cab.mailbox

import kotlin.math.roundToInt

/** Reuse a recent fix on send; do not wait for a new satellite lock. */
const val GEO_CACHE_MS = 120_000L
/** lastLocation is cached in Play Services; keep the send path snappy. */
const val GEO_LAST_KNOWN_MS = 250L

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

/** Text send: only show a hint when a pin actually went out. Omitted is not a failed send. */
fun sendGeoHint(enabled: Boolean, geo: Geo?): String? {
  if (!enabled || geo == null) {
    return null
  }
  return geoHint(true, geo)
}

/** Clamp to what the Durable Object will accept so the pin is not `bad frame`. */
fun geoFromFix(
  lat: Double,
  lon: Double,
  accuracyM: Double? = null,
  altM: Double? = null,
  heading: Double? = null,
  speedMps: Double? = null,
): Geo = Geo(
  lat = lat,
  lon = lon,
  accuracyM = accuracyM?.takeIf { it >= 0.0 },
  altM = altM,
  heading = heading?.takeIf { it >= 0.0 && it < 360.0 },
  speedMps = speedMps?.takeIf { it >= 0.0 },
)
