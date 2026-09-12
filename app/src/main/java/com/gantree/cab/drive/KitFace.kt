package com.gantree.cab.drive

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.gantree.cab.mailbox.JpegCheck
import com.gantree.cab.mailbox.acceptJpeg
import com.gantree.cab.mailbox.displaySlug

/** Auto copies this across processes; keep it well under the binder cap. */
const val KIT_FACE_EDGE = 256

/**
 * JPEG from `/api/avatar` into a square bitmap Auto can paint on a
 * `Person`. Vector shortcut icons and a missing [Person.Builder.setIcon]
 * both become a letter (the big K). Null / junk stays null — letter is
 * the right fallback when the room has no face.
 */
fun kitFaceBitmap(jpeg: ByteArray?): Bitmap? {
  if (jpeg == null || jpeg.isEmpty()) {
    return null
  }
  if (acceptJpeg(jpeg) !is JpegCheck.Ok) {
    return null
  }
  val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
  BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, bounds)
  val longest = maxOf(bounds.outWidth, bounds.outHeight)
  if (longest <= 0) {
    return null
  }
  var sample = 1
  while (longest / sample > KIT_FACE_EDGE * 2) {
    sample *= 2
  }
  val opts = BitmapFactory.Options().apply { inSampleSize = sample }
  val src = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, opts) ?: return null
  val w = src.width
  val h = src.height
  if (w <= 0 || h <= 0) {
    return null
  }
  val side = minOf(w, h)
  val left = (w - side) / 2
  val top = (h - side) / 2
  val dst = Bitmap.createBitmap(KIT_FACE_EDGE, KIT_FACE_EDGE, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(dst)
  canvas.drawBitmap(
    src,
    Rect(left, top, left + side, top + side),
    Rect(0, 0, KIT_FACE_EDGE, KIT_FACE_EDGE),
    Paint(Paint.FILTER_BITMAP_FLAG),
  )
  return dst
}

fun kitFaceIcon(jpeg: ByteArray?): IconCompat? =
  kitFaceBitmap(jpeg)?.let { IconCompat.createWithBitmap(it) }

fun kitPerson(slug: String, icon: IconCompat? = null): Person {
  val b = Person.Builder().setName(displaySlug(slug)).setKey(CabNotifier.conversationId(slug))
  if (icon != null) {
    b.setIcon(icon)
  }
  return b.build()
}
