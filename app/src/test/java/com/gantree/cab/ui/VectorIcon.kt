package com.gantree.cab.ui

import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.geom.Arc2D
import java.awt.geom.Path2D
import java.io.File
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val VIEW = 24.0

fun paintVectorDrawable(g: Graphics2D, name: String, cx: Float, cy: Float, size: Int, color: Color) {
  val shapes = vectorShapes(name)
  val g2 = g.create() as Graphics2D
  g2.translate((cx - size / 2f).toDouble(), (cy - size / 2f).toDouble())
  g2.scale(size / VIEW, size / VIEW)
  g2.color = color
  for (shape in shapes) {
    g2.fill(shape)
  }
  g2.dispose()
}

fun vectorShapes(name: String): List<Path2D> {
  val xml = drawableFile(name).readText()
  return PATH_DATA.findAll(xml).map { parsePath(it.groupValues[1]) }.toList()
}

private val PATH_DATA = Regex("""android:pathData="([^"]+)"""")

private fun drawableFile(name: String): File = listOf(
  File("src/main/res/drawable/$name.xml"),
  File("app/src/main/res/drawable/$name.xml"),
).first { it.isFile }

private fun parsePath(data: String): Path2D {
  val p = Path2D.Float()
  val s = PathScanner(data)
  var cx = 0f
  var cy = 0f
  var subX = 0f
  var subY = 0f
  var lastCmd = ' '
  var prevCtrlX = 0f
  var prevCtrlY = 0f
  var hasPrevCubic = false
  while (true) {
    s.skip()
    if (s.done) break
    val cmd = if (s.isCommand) {
      s.command()
    } else if (lastCmd == ' ') {
      break
    } else if (lastCmd == 'M') {
      'L'
    } else if (lastCmd == 'm') {
      'l'
    } else {
      lastCmd
    }
    lastCmd = cmd
    when (cmd) {
      'M' -> {
        cx = s.num(); cy = s.num()
        p.moveTo(cx, cy)
        subX = cx; subY = cy
        hasPrevCubic = false
      }
      'm' -> {
        cx += s.num(); cy += s.num()
        p.moveTo(cx, cy)
        subX = cx; subY = cy
        hasPrevCubic = false
      }
      'L' -> {
        cx = s.num(); cy = s.num()
        p.lineTo(cx, cy)
        hasPrevCubic = false
      }
      'l' -> {
        cx += s.num(); cy += s.num()
        p.lineTo(cx, cy)
        hasPrevCubic = false
      }
      'H' -> {
        cx = s.num()
        p.lineTo(cx, cy)
        hasPrevCubic = false
      }
      'h' -> {
        cx += s.num()
        p.lineTo(cx, cy)
        hasPrevCubic = false
      }
      'V' -> {
        cy = s.num()
        p.lineTo(cx, cy)
        hasPrevCubic = false
      }
      'v' -> {
        cy += s.num()
        p.lineTo(cx, cy)
        hasPrevCubic = false
      }
      'C' -> {
        val x1 = s.num(); val y1 = s.num()
        val x2 = s.num(); val y2 = s.num()
        cx = s.num(); cy = s.num()
        p.curveTo(x1, y1, x2, y2, cx, cy)
        prevCtrlX = x2; prevCtrlY = y2
        hasPrevCubic = true
      }
      'c' -> {
        val x1 = cx + s.num(); val y1 = cy + s.num()
        val x2 = cx + s.num(); val y2 = cy + s.num()
        cx += s.num(); cy += s.num()
        p.curveTo(x1, y1, x2, y2, cx, cy)
        prevCtrlX = x2; prevCtrlY = y2
        hasPrevCubic = true
      }
      'S' -> {
        val x1 = if (hasPrevCubic) 2 * cx - prevCtrlX else cx
        val y1 = if (hasPrevCubic) 2 * cy - prevCtrlY else cy
        val x2 = s.num(); val y2 = s.num()
        cx = s.num(); cy = s.num()
        p.curveTo(x1, y1, x2, y2, cx, cy)
        prevCtrlX = x2; prevCtrlY = y2
        hasPrevCubic = true
      }
      's' -> {
        val x1 = if (hasPrevCubic) 2 * cx - prevCtrlX else cx
        val y1 = if (hasPrevCubic) 2 * cy - prevCtrlY else cy
        val x2 = cx + s.num(); val y2 = cy + s.num()
        cx += s.num(); cy += s.num()
        p.curveTo(x1, y1, x2, y2, cx, cy)
        prevCtrlX = x2; prevCtrlY = y2
        hasPrevCubic = true
      }
      'A' -> {
        val rx = s.num(); val ry = s.num(); val phi = s.num()
        val large = s.flag(); val sweep = s.flag()
        val x = s.num(); val y = s.num()
        arcTo(p, cx, cy, rx, ry, phi, large, sweep, x, y)
        cx = x; cy = y
        hasPrevCubic = false
      }
      'a' -> {
        val rx = s.num(); val ry = s.num(); val phi = s.num()
        val large = s.flag(); val sweep = s.flag()
        val x = cx + s.num(); val y = cy + s.num()
        arcTo(p, cx, cy, rx, ry, phi, large, sweep, x, y)
        cx = x; cy = y
        hasPrevCubic = false
      }
      'Z', 'z' -> {
        p.closePath()
        cx = subX; cy = subY
        hasPrevCubic = false
      }
      else -> error("unsupported path command $cmd")
    }
  }
  return p
}

private class PathScanner(private val d: String) {
  var i = 0
  val done: Boolean get() { skip(); return i >= d.length }
  val isCommand: Boolean get() { skip(); return i < d.length && d[i].isLetter() }

  fun skip() {
    while (i < d.length) {
      val c = d[i]
      if (c == ' ' || c == ',' || c == '\n' || c == '\t' || c == '\r') i++ else break
    }
  }

  fun command(): Char {
    skip()
    val c = d[i]
    i++
    return c
  }

  fun flag(): Boolean = num() != 0f

  fun num(): Float {
    skip()
    val start = i
    if (i < d.length && (d[i] == '+' || d[i] == '-')) i++
    while (i < d.length && d[i] in '0'..'9') i++
    if (i < d.length && d[i] == '.') {
      i++
      while (i < d.length && d[i] in '0'..'9') i++
    }
    if (i < d.length && (d[i] == 'e' || d[i] == 'E')) {
      i++
      if (i < d.length && (d[i] == '+' || d[i] == '-')) i++
      while (i < d.length && d[i] in '0'..'9') i++
    }
    require(i > start) { "expected number at ${d.substring(i)}" }
    return d.substring(start, i).toFloat()
  }
}

/** SVG elliptical arc → Java2D. Rotation is unused (our icons are axis-aligned). */
private fun arcTo(
  p: Path2D,
  x1: Float,
  y1: Float,
  rx0: Float,
  ry0: Float,
  phiDeg: Float,
  large: Boolean,
  sweep: Boolean,
  x2: Float,
  y2: Float,
) {
  var rx = abs(rx0.toDouble())
  var ry = abs(ry0.toDouble())
  if (rx == 0.0 || ry == 0.0) {
    p.lineTo(x2.toDouble(), y2.toDouble())
    return
  }
  val phi = Math.toRadians(phiDeg.toDouble())
  val cosPhi = cos(phi)
  val sinPhi = sin(phi)
  val dx = (x1 - x2) / 2.0
  val dy = (y1 - y2) / 2.0
  val x1p = cosPhi * dx + sinPhi * dy
  val y1p = -sinPhi * dx + cosPhi * dy
  var rxs = rx * rx
  var rys = ry * ry
  val x1ps = x1p * x1p
  val y1ps = y1p * y1p
  val lambda = x1ps / rxs + y1ps / rys
  if (lambda > 1) {
    val m = sqrt(lambda)
    rx *= m
    ry *= m
    rxs = rx * rx
    rys = ry * ry
  }
  val sq = max(0.0, (rxs * rys - rxs * y1ps - rys * x1ps) / (rxs * y1ps + rys * x1ps))
  val sign = if (large == sweep) -1.0 else 1.0
  val cxp = sign * sqrt(sq) * rx * y1p / ry
  val cyp = sign * -sqrt(sq) * ry * x1p / rx
  val cx = cosPhi * cxp - sinPhi * cyp + (x1 + x2) / 2.0
  val cy = sinPhi * cxp + cosPhi * cyp + (y1 + y2) / 2.0
  fun angle(ux: Double, uy: Double, vx: Double, vy: Double): Double {
    val mag = sqrt(ux * ux + uy * uy) * sqrt(vx * vx + vy * vy)
    if (mag == 0.0) return 0.0
    val c = min(1.0, max(-1.0, (ux * vx + uy * vy) / mag))
    val a = acos(c)
    return if (ux * vy - uy * vx < 0) -a else a
  }
  val start = angle(1.0, 0.0, (x1p - cxp) / rx, (y1p - cyp) / ry)
  var delta = angle((x1p - cxp) / rx, (y1p - cyp) / ry, (-x1p - cxp) / rx, (-y1p - cyp) / ry)
  if (!sweep && delta > 0) delta -= 2 * Math.PI
  if (sweep && delta < 0) delta += 2 * Math.PI
  val arc = Arc2D.Double(
    cx - rx,
    cy - ry,
    rx * 2,
    ry * 2,
    Math.toDegrees(-start),
    Math.toDegrees(-delta),
    Arc2D.OPEN,
  )
  val at = AffineTransform()
  at.rotate(phi, cx, cy)
  p.append(at.createTransformedShape(arc), true)
}

internal const val VEC_ATTACH = "ic_attach_file"
internal const val VEC_SMILE = "ic_sentiment_satisfied"
internal const val VEC_PHOTO = "ic_photo"
internal const val VEC_CAMERA = "ic_photo_camera"
internal const val VEC_CODE = "ic_code"
internal const val VEC_PIN = "ic_add_location_alt"
