package com.gantree.cab.ui

/**
 * Runtime grants the Settings → Access block paints. Read by the Activity
 * (`checkSelfPermission`) on create, resume, and after each prompt; never
 * asked for here. Pendant `MicEnable` / `GeoEnable` / `NotifyEnable`.
 */
data class Permits(
  val mic: Boolean = false,
  val location: Boolean = false,
  val notify: Boolean = false,
)
