package com.gantree.cab.drive

data class KitTurn(
  val text: String,
  val at: Long,
)

fun pushKitTurn(
  prev: List<KitTurn>,
  prevSlug: String,
  slug: String,
  text: String,
  at: Long,
  cap: Int = 6,
): Pair<String, List<KitTurn>> {
  val base = if (prevSlug == slug) prev else emptyList()
  return slug to (base + KitTurn(text, at)).takeLast(cap)
}
