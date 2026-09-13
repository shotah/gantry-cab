package com.gantree.cab.mailbox

fun displaySlug(slug: String): String {
  val s = slug.trim()
  if (s.isEmpty()) {
    return "Kit"
  }
  return s.replaceFirstChar { it.uppercase() }
}

/**
 * Worker blob revs are `Date.now()` (epoch ms). That does not fit in Int;
 * dropping those notices left wallpaper stuck while the face still moved
 * (face travels as a decimal string and was truncated instead).
 */
fun foldBlobRev(n: Long): Int? {
  if (n < 0L) {
    return null
  }
  if (n == 0L) {
    return 0
  }
  if (n <= Int.MAX_VALUE) {
    return n.toInt()
  }
  val folded = (n and 0x7FFFFFFF).toInt()
  return if (folded == 0) 1 else folded
}

fun faceRev(kind: String?, text: String?): Int? {
  if (kind != "face") {
    return null
  }
  val n = text?.toLongOrNull() ?: return null
  return foldBlobRev(n)?.takeIf { it > 0 }
}

/** `rev` 0 = cleared. Junk (missing, negative, not an int) is not a backdrop notice. */
fun backdropRev(kind: String?, raw: Any?): Int? {
  if (kind != "backdrop") {
    return null
  }
  val n = jsonWholeNumber(raw) ?: (raw as? String)?.toLongOrNull() ?: return null
  return foldBlobRev(n)
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

/** `If-None-Match` for a blob we hold; pendant `blobEtag`. */
fun blobEtag(rev: Int): String = "\"$rev\""

/** Rev a blob GET names itself with: `X-Pendant-Rev`, else the `ETag` digits. 0 = unknown. */
fun blobRev(xRev: String?, etag: String?): Int {
  headerBlobRev(xRev)?.let { return it }
  return headerBlobRev(etag?.trim()?.removePrefix("W/")?.trim('"')) ?: 0
}

private fun headerBlobRev(raw: String?): Int? =
  foldBlobRev(raw?.trim()?.toLongOrNull() ?: return null)?.takeIf { it > 0 }

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
