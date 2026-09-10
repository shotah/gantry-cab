package com.gantree.cab.mailbox

fun displaySlug(slug: String): String {
  val s = slug.trim()
  if (s.isEmpty()) {
    return "Kit"
  }
  return s.replaceFirstChar { it.uppercase() }
}

fun faceRev(kind: String?, text: String?): Int? {
  if (kind != "face") {
    return null
  }
  val n = text?.toLongOrNull() ?: return null
  return if (n > 0) n.toInt() else null
}

fun avatarUrl(origin: String, slug: String, rev: Int = 0): String {
  val base = "${httpOrigin(origin)}/api/avatar?slug=$slug"
  return if (rev > 0) "$base&v=$rev" else base
}
