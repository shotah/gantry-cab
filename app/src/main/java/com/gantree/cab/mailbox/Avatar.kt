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

/** `rev` 0 = cleared. Junk (missing, negative, not an int) is not a backdrop notice. */
fun backdropRev(kind: String?, raw: Any?): Int? {
  if (kind != "backdrop") {
    return null
  }
  val n = jsonWholeNumber(raw) ?: return null
  if (n < 0 || n > Int.MAX_VALUE) {
    return null
  }
  return n.toInt()
}

/**
 * Known id, empty string when cleared, null when this is not a theme notice (or junk).
 * Mirrors pendant `themeIdFromUnknown`.
 */
fun roomThemeNotice(kind: String?, themePresent: Boolean, themeNull: Boolean, themeRaw: String?): String? {
  if (kind != "theme") {
    return null
  }
  if (!themePresent || themeNull) {
    return ""
  }
  return knownTheme(themeRaw)
}

fun blobUrl(origin: String, path: String, slug: String, rev: Int = 0): String {
  val base = "${httpOrigin(origin)}$path?slug=$slug"
  return if (rev > 0) "$base&v=$rev" else base
}

fun avatarUrl(origin: String, slug: String, rev: Int = 0): String =
  blobUrl(origin, "/api/avatar", slug, rev)

fun backdropUrl(origin: String, slug: String, rev: Int = 0): String =
  blobUrl(origin, "/api/backdrop", slug, rev)

fun themeUrl(origin: String, slug: String): String =
  "${httpOrigin(origin)}/api/theme?slug=$slug"
