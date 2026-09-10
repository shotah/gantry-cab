package com.gantree.cab.ui

import com.gantree.cab.dev.sampleScene
import com.gantree.cab.drive.carRows
import com.gantree.cab.mailbox.displaySlug
import com.gantree.cab.mailbox.searchEmoji
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Arc2D
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

private val Canvas = Color(0x0E1316)
private val Panel = Color(0x171D22)
private val Track = Color(0x232B32)
private val Body = Color(0xDCD6CE)
private val Fg = Color(0xF4F0EA)
private val Muted = Color(0x9AA3AB)
private val Dim = Color(0x84909A)
private val Accent = Color(0xF07848)
private val Live = Color(0x3DB8A0)
private val You = Color(0x3A1E16)
private val Kit = Color(0x232B32)
private val Field = Color(0x3A4550)
private val AccentSoft = Color(0x2A1612)
private val Mark = Color(0xF3B199)
private val CarBg = Color(0x121212)
private val CarLine = Color(0x2A2A2A)
private val CarTitle = Color(0xF2F2F2)
private val CarBody = Color(0xB8B8B8)

const val PHONE_W = 390
const val PHONE_H = 844
const val AUTO_W = 1024
const val AUTO_H = 576

val DOCS_SHOT_NAMES = listOf(
  "phone-unsigned",
  "phone-empty",
  "phone-thread",
  "phone-down",
  "phone-settings",
  "phone-emoji",
  "auto-empty",
  "auto-thread",
)

fun renderDocsShot(name: String): BufferedImage = when (name) {
  "phone-unsigned" -> renderPhone("unsigned")
  "phone-empty" -> renderPhone("empty")
  "phone-thread" -> renderPhone("thread")
  "phone-down" -> renderPhone("down")
  "phone-settings" -> renderPhone("empty", settings = true)
  "phone-emoji" -> renderPhone("thread", emoji = true)
  "auto-empty" -> renderAuto("empty")
  "auto-thread" -> renderAuto("thread")
  else -> error("unknown shot $name")
}

fun writeDocsShots(dir: File) {
  dir.mkdirs()
  for (name in DOCS_SHOT_NAMES) {
    ImageIO.write(renderDocsShot(name), "png", File(dir, "$name.png"))
  }
}

fun renderPhone(sampleId: String, settings: Boolean = false, emoji: Boolean = false): BufferedImage {
  val scene = sampleScene(sampleId) ?: error("sample $sampleId")
  val img = BufferedImage(PHONE_W, PHONE_H, BufferedImage.TYPE_INT_ARGB)
  val g = img.graphics2d()
  val font = noto()
  val door = sampleId == "unsigned"
  g.color = Canvas
  g.fillRect(0, 0, PHONE_W, PHONE_H)
  paintHeader(g, font, scene.slug, scene.up, settings)
  var y = 64
  if (!door && !settings && scene.hint.isNotBlank()) {
    g.font = font.deriveFont(12f)
    g.color = Muted
    y = g.drawLine(scene.hint, 16, y + 4, 12)
  }
  val composerTop = if (emoji) PHONE_H - 268 else PHONE_H - 84
  if (door) {
    paintAvatar(g, (PHONE_W - 64) / 2f, 280f, 64f)
    g.font = font.deriveFont(16f)
    g.color = Body
    val msg = "Sign in with Google to talk."
    g.drawString(msg, (PHONE_W - g.fontMetrics.stringWidth(msg)) / 2, 370)
    val btn = "Continue with Google"
    g.font = font.deriveFont(14f)
    val bw = g.fontMetrics.stringWidth(btn) + 32
    val bx = (PHONE_W - bw) / 2
    g.color = AccentSoft
    g.fill(RoundRectangle2D.Float(bx.toFloat(), 386f, bw.toFloat(), 40f, 12f, 12f))
    g.color = Mark
    g.drawString(btn, bx + 16, 412)
  } else {
    if (settings) {
      y = paintSettings(g, font, scene.slug, scene.email, y)
    } else if (scene.lines.isEmpty()) {
      paintAvatar(g, (PHONE_W - 64) / 2f, 180f, 64f)
      g.font = font.deriveFont(14f)
      g.color = Dim
      val empty = "Nothing yet. Type below — or / for harness commands."
      val wrapped = wrap(g, empty, PHONE_W - 64)
      var ty = 260
      for (w in wrapped) {
        g.drawString(w, (PHONE_W - g.fontMetrics.stringWidth(w)) / 2, ty)
        ty += 18
      }
    } else {
      val bubbleMax = PHONE_W - 16 - 48 - 24
      g.font = font.deriveFont(16f)
      for (line in scene.lines) {
        val wrapped = wrap(g, line.text, bubbleMax)
        val bh = 12 + wrapped.size * 20 + 12
        val bw = (wrapped.maxOf { g.fontMetrics.stringWidth(it) } + 24).coerceAtMost(PHONE_W - 64)
        val x = if (line.fromYou) PHONE_W - 16 - bw else 16
        g.color = if (line.fromYou) You else Kit
        g.fill(RoundRectangle2D.Float(x.toFloat(), y.toFloat(), bw.toFloat(), bh.toFloat(), 24f, 24f))
        g.color = Fg
        var ty = y + 12
        for (w in wrapped) {
          g.drawString(w, x + 12, ty + g.fontMetrics.ascent)
          ty += 20
        }
        y += bh + 8
        if (y > composerTop - 8) break
      }
    }
    if (emoji) {
      paintEmojiPanel(g, font, composerTop - 176)
    }
    paintComposer(g, font, composerTop, scene.slug, emoji)
  }
  g.dispose()
  return img
}

fun renderAuto(sampleId: String): BufferedImage {
  val scene = sampleScene(sampleId) ?: error("sample $sampleId")
  val rows = carRows(scene.lines, scene.slug, "No messages yet. Speak a reply when Kit pings you.")
  val img = BufferedImage(AUTO_W, AUTO_H, BufferedImage.TYPE_INT_ARGB)
  val g = img.graphics2d()
  val font = noto()
  g.color = CarBg
  g.fillRect(0, 0, AUTO_W, AUTO_H)
  var y = 28
  g.font = font.deriveFont(28f)
  g.color = Accent
  y = g.drawLine(scene.slug.ifBlank { "cab" }, 48, y, 28)
  y += 12
  g.color = CarLine
  g.stroke = BasicStroke(1f)
  g.drawLine(48, y, AUTO_W - 48, y)
  y += 20
  for (row in rows) {
    g.font = font.deriveFont(22f)
    g.color = CarTitle
    y = g.drawLine(row.title, 48, y, 22)
    val body = row.text
    if (!body.isNullOrBlank()) {
      g.font = font.deriveFont(16f)
      g.color = CarBody
      for (w in wrap(g, body, AUTO_W - 96)) {
        y = g.drawLine(w, 48, y + 4, 16)
      }
    }
    y += 16
  }
  g.dispose()
  return img
}

private fun paintHeader(g: Graphics2D, font: Font, slug: String, up: Boolean, settingsOpen: Boolean) {
  g.color = Panel
  g.fillRect(0, 0, PHONE_W, 56)
  paintAvatar(g, 12f, 8f, 40f)
  g.font = font.deriveFont(16f)
  g.color = Fg
  g.drawString(displaySlug(slug), 60, 28)
  g.font = font.deriveFont(11f)
  g.color = if (up) Live else Dim
  g.drawString(if (up) "live" else "down", 60, 44)
  paintCog(g, PHONE_W - 26f, 28f, if (settingsOpen) Fg else Muted)
}

private fun paintAvatar(g: Graphics2D, x: Float, y: Float, size: Float) {
  g.color = Track
  g.fill(Ellipse2D.Float(x, y, size, size))
  val cx = x + size / 2f
  val cy = y + size / 2f
  val s = size / 40f
  g.color = Muted
  g.fill(Ellipse2D.Float(cx - 5.5f * s, cy - 4f * s, 4f * s, 4f * s))
  g.fill(Ellipse2D.Float(cx + 1.5f * s, cy - 4f * s, 4f * s, 4f * s))
  g.stroke = BasicStroke((1.6f * s).coerceAtLeast(1.2f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
  g.draw(Arc2D.Float(cx - 8f * s, cy - 2f * s, 16f * s, 14f * s, 200f, 140f, Arc2D.OPEN))
}

private fun paintCog(g: Graphics2D, cx: Float, cy: Float, color: Color) {
  val r = 8f
  val saved = g.transform
  g.color = color
  for (i in 0 until 6) {
    g.rotate(Math.PI / 3.0 * i, cx.toDouble(), cy.toDouble())
    g.fill(RoundRectangle2D.Float(cx - 2.2f, cy - r - 3.5f, 4.4f, 7.5f, 1.5f, 1.5f))
    g.transform = saved
  }
  g.fill(Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2))
  g.color = Panel
  g.fill(Ellipse2D.Float(cx - 3.4f, cy - 3.4f, 6.8f, 6.8f))
}

private fun paintSettings(g: Graphics2D, font: Font, slug: String, email: String, startY: Int): Int {
  g.color = Panel
  g.fillRect(0, 56, PHONE_W, 380)
  var y = startY
  y = g.field(font, "Mailbox origin", "http://10.0.2.2:3000", 12, y)
  y = g.field(font, "Agent name", slug, 12, y + 6)
  y = g.field(font, "Agent access secret", "••••••••", 12, y + 6)
  g.font = font.deriveFont(12f)
  g.color = Muted
  y = g.drawLine("Theme", 16, y + 10, 12)
  var x = 16
  for (label in listOf("Boom", "Inlay", "Lamp")) {
    val on = label == "Boom"
    g.color = if (on) Accent else Field
    g.stroke = BasicStroke(1.2f)
    g.draw(RoundRectangle2D.Float(x.toFloat(), y.toFloat(), 72f, 26f, 8f, 8f))
    g.color = Body
    g.font = font.deriveFont(12f)
    g.drawString(label, x + 10, y + 18)
    x += 80
  }
  y += 36
  g.color = Muted
  y = g.drawLine("Font size", 16, y, 12)
  x = 16
  for (size in listOf(12f, 14f, 16f, 18f)) {
    val on = size == 12f
    g.color = if (on) Field else Canvas
    g.fill(RoundRectangle2D.Float(x.toFloat(), y.toFloat(), 78f, 32f, 8f, 8f))
    g.color = if (on) Accent else Field
    g.stroke = BasicStroke(1.2f)
    g.draw(RoundRectangle2D.Float(x.toFloat(), y.toFloat(), 78f, 32f, 8f, 8f))
    g.font = font.deriveFont(size)
    g.color = Fg
    val aa = "Aa"
    g.drawString(aa, x + (78 - g.fontMetrics.stringWidth(aa)) / 2, y + 22)
    x += 86
  }
  y += 44
  g.color = Accent
  g.fill(RoundRectangle2D.Float(16f, y.toFloat(), 88f, 36f, 12f, 12f))
  g.font = font.deriveFont(14f)
  g.color = Canvas
  g.drawString("Listen", 36, y + 24)
  if (email.isNotBlank()) {
    g.font = font.deriveFont(12f)
    g.color = Dim
    g.drawString("sign out", 120, y + 24)
    y += 40
    g.color = Muted
    y = g.drawLine(email, 16, y, 12)
  }
  return y + 8
}

private fun Graphics2D.field(font: Font, label: String, value: String, x: Int, y: Int): Int {
  this.font = font.deriveFont(11f)
  color = Muted
  val afterLabel = drawLine(label, x, y, 11)
  color = Field
  stroke = BasicStroke(1.2f)
  draw(RoundRectangle2D.Float(x.toFloat(), afterLabel.toFloat(), (PHONE_W - 32).toFloat(), 32f, 8f, 8f))
  this.font = font.deriveFont(14f)
  color = Fg
  drawString(value, x + 10, afterLabel + 22)
  return afterLabel + 32
}

private fun paintEmojiPanel(g: Graphics2D, font: Font, top: Int) {
  val faces = searchEmoji("").take(24)
  g.color = Panel
  g.fill(RoundRectangle2D.Float(12f, top.toFloat(), (PHONE_W - 24).toFloat(), 168f, 12f, 12f))
  g.color = Field
  g.stroke = BasicStroke(1.2f)
  g.draw(RoundRectangle2D.Float(12f, top.toFloat(), (PHONE_W - 24).toFloat(), 168f, 12f, 12f))
  g.color = Canvas
  g.fill(RoundRectangle2D.Float(22f, (top + 10).toFloat(), (PHONE_W - 44).toFloat(), 30f, 8f, 8f))
  g.font = font.deriveFont(13f)
  g.color = Muted
  g.drawString("Search or :shrug:", 32, top + 30)
  var i = 0
  for (row in 0 until 3) {
    for (col in 0 until 8) {
      if (i >= faces.size) break
      val x = 26f + col * 44f
      val ey = top + 58f + row * 36f
      paintEmojiGlyph(g, faces[i].name, x, ey, 24f)
      i += 1
    }
  }
}

private fun paintComposer(g: Graphics2D, font: Font, composerTop: Int, slug: String, emojiOpen: Boolean) {
  g.color = Panel
  g.fillRect(0, composerTop - 12, PHONE_W, PHONE_H - composerTop + 12)
  g.color = Field
  g.stroke = BasicStroke(1.5f)
  val sendW = 76
  val fieldX = 16f
  val fieldW = (PHONE_W - 16 - sendW - 12).toFloat()
  g.draw(RoundRectangle2D.Float(fieldX, composerTop.toFloat(), fieldW, 52f, 12f, 12f))
  paintFace(g, 26f, composerTop + 6f, emojiOpen)
  paintClip(g, 26f, composerTop + 28f)
  g.font = font.deriveFont(14f)
  g.color = Muted
  g.drawString("Message ${displaySlug(slug)}", 48, composerTop + 32)
  val sendX = PHONE_W - 16 - sendW
  g.color = Accent
  g.fill(RoundRectangle2D.Float(sendX.toFloat(), composerTop.toFloat(), sendW.toFloat(), 52f, 22f, 22f))
  g.font = font.deriveFont(14f)
  g.color = Canvas
  val send = "Send"
  g.drawString(send, sendX + (sendW - g.fontMetrics.stringWidth(send)) / 2, composerTop + 32)
}

private fun paintFace(g: Graphics2D, x: Float, y: Float, on: Boolean) {
  g.color = if (on) Accent else Muted
  g.stroke = BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
  g.draw(Ellipse2D.Float(x, y, 16f, 16f))
  g.fill(Ellipse2D.Float(x + 4.2f, y + 5f, 2.4f, 2.4f))
  g.fill(Ellipse2D.Float(x + 9.4f, y + 5f, 2.4f, 2.4f))
  g.draw(Arc2D.Float(x + 3.5f, y + 6f, 9f, 7f, 200f, 140f, Arc2D.OPEN))
}

private fun paintClip(g: Graphics2D, x: Float, y: Float) {
  g.color = Muted
  g.stroke = BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
  val path = Path2D.Float()
  path.moveTo(x + 4f, y + 9f)
  path.curveTo(x + 4f, y + 4f, x + 12f, y + 4f, x + 12f, y + 9f)
  path.lineTo(x + 12f, y + 13f)
  path.curveTo(x + 12f, y + 16.5f, x + 7f, y + 16.5f, x + 7f, y + 13f)
  path.lineTo(x + 7f, y + 7.5f)
  g.draw(path)
}

private fun Graphics2D.drawLine(text: String, x: Int, y: Int, size: Int): Int {
  drawString(text, x, y + fontMetrics.ascent)
  return y + size + 4
}

private fun wrap(g: Graphics2D, text: String, maxWidth: Int): List<String> {
  val fm = g.fontMetrics
  val words = text.split(' ')
  val lines = mutableListOf<String>()
  var cur = ""
  for (word in words) {
    val next = if (cur.isEmpty()) word else "$cur $word"
    if (fm.stringWidth(next) <= maxWidth) {
      cur = next
    } else {
      if (cur.isNotEmpty()) lines.add(cur)
      cur = word
    }
  }
  if (cur.isNotEmpty()) lines.add(cur)
  return lines.ifEmpty { listOf("") }
}

private fun BufferedImage.graphics2d(): Graphics2D {
  val g = createGraphics()
  g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
  g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
  g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
  return g
}

private val Face = Color(0xF5C542)
private val Skin = Color(0xF3C6A0)
private val Heart = Color(0xE24B4A)
private val FireHi = Color(0xF07848)
private val Ink = Color(0x2A2420)

private fun paintEmojiGlyph(g: Graphics2D, name: String, x: Float, y: Float, size: Float) {
  val saved = g.stroke
  g.stroke = BasicStroke((size / 12f).coerceAtLeast(1.2f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
  when (name) {
    "thumbsup" -> paintThumb(g, x, y, size, true)
    "thumbsdown" -> paintThumb(g, x, y, size, false)
    "ok_hand" -> paintOkHand(g, x, y, size)
    "clap" -> paintClap(g, x, y, size)
    "pray" -> paintPray(g, x, y, size)
    "wave" -> paintWave(g, x, y, size)
    "muscle" -> paintMuscle(g, x, y, size)
    "heart" -> paintHeart(g, x + size * 0.08f, y + size * 0.12f, size * 0.84f)
    "fire" -> paintFire(g, x, y, size)
    "shrug" -> paintShrug(g, x, y, size)
    "facepalm" -> paintFacepalm(g, x, y, size)
    else -> paintFaceGlyph(g, name, x, y, size)
  }
  g.stroke = saved
}

private fun paintFaceGlyph(g: Graphics2D, name: String, x: Float, y: Float, size: Float) {
  val fill = when (name) {
    "rage" -> Color(0xE05A32)
    else -> Face
  }
  g.color = fill
  g.fill(Ellipse2D.Float(x, y, size, size))
  val cx = x + size / 2f
  val cy = y + size / 2f
  val s = size / 24f
  if (name == "sunglasses") {
    g.color = Ink
    g.fill(RoundRectangle2D.Float(cx - 9f * s, cy - 3.2f * s, 7.2f * s, 5.2f * s, 2f * s, 2f * s))
    g.fill(RoundRectangle2D.Float(cx + 1.8f * s, cy - 3.2f * s, 7.2f * s, 5.2f * s, 2f * s, 2f * s))
    g.stroke = BasicStroke(1.4f * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    g.drawLine((cx - 1.8f * s).toInt(), (cy - 0.6f * s).toInt(), (cx + 1.8f * s).toInt(), (cy - 0.6f * s).toInt())
  } else if (name == "heart_eyes") {
    paintHeart(g, cx - 8.6f * s, cy - 6.2f * s, 6.4f * s)
    paintHeart(g, cx + 2.2f * s, cy - 6.2f * s, 6.4f * s)
  } else if (name == "wink") {
    g.color = Ink
    g.fill(Ellipse2D.Float(cx - 6.4f * s, cy - 3.6f * s, 3.2f * s, 3.2f * s))
    g.drawLine((cx + 3.2f * s).toInt(), (cy - 2.4f * s).toInt(), (cx + 6.8f * s).toInt(), (cy - 1.4f * s).toInt())
  } else if (name == "thinking") {
    g.color = Ink
    g.fill(Ellipse2D.Float(cx - 6.4f * s, cy - 3.8f * s, 3f * s, 3.6f * s))
    g.fill(Ellipse2D.Float(cx + 3.2f * s, cy - 3.2f * s, 3f * s, 3f * s))
    g.draw(Arc2D.Float(cx - 5f * s, cy + 1f * s, 8f * s, 6f * s, 200f, 90f, Arc2D.OPEN))
    g.color = Skin
    g.fill(Ellipse2D.Float(cx + 6.5f * s, cy + 5.5f * s, 5.5f * s, 5.5f * s))
  } else {
    g.color = Ink
    val eyeH = if (name == "joy" || name == "sob") 1.6f * s else 3.1f * s
    g.fill(Ellipse2D.Float(cx - 6.2f * s, cy - 4.2f * s, 3.1f * s, eyeH))
    g.fill(Ellipse2D.Float(cx + 3.1f * s, cy - 4.2f * s, 3.1f * s, eyeH))
  }
  if (name == "blush") {
    g.color = Color(0xE89A8A)
    g.fill(Ellipse2D.Float(cx - 9.5f * s, cy + 1.2f * s, 5.5f * s, 3.2f * s))
    g.fill(Ellipse2D.Float(cx + 4f * s, cy + 1.2f * s, 5.5f * s, 3.2f * s))
  }
  if (name == "partying") {
    g.color = Accent
    val hat = Path2D.Float()
    hat.moveTo(cx, y - 1.5f * s)
    hat.lineTo(cx + 8f * s, cy - 6f * s)
    hat.lineTo(cx - 2f * s, cy - 8f * s)
    hat.closePath()
    g.fill(hat)
  }
  if (name != "sunglasses" && name != "thinking") {
    g.color = Ink
    when (name) {
      "smile", "grinning" -> g.draw(Arc2D.Float(cx - 7f * s, cy - 1f * s, 14f * s, 12f * s, 200f, 140f, Arc2D.OPEN))
      "joy" -> {
        g.fill(Ellipse2D.Float(cx - 5.5f * s, cy + 1.5f * s, 11f * s, 7f * s))
        g.color = Color(0x6EC8E8)
        g.fill(Ellipse2D.Float(cx - 9.5f * s, cy + 2f * s, 2.6f * s, 5.5f * s))
        g.fill(Ellipse2D.Float(cx + 6.8f * s, cy + 2f * s, 2.6f * s, 5.5f * s))
      }
      "cry" -> {
        g.draw(Arc2D.Float(cx - 6f * s, cy + 2f * s, 12f * s, 8f * s, 20f, 140f, Arc2D.OPEN))
        g.color = Color(0x6EC8E8)
        g.fill(Ellipse2D.Float(cx - 6.4f * s, cy + 1.5f * s, 2.4f * s, 6f * s))
      }
      "sob" -> {
        g.fill(Ellipse2D.Float(cx - 5f * s, cy + 1.2f * s, 11f * s, 7.5f * s))
        g.color = Color(0x6EC8E8)
        g.fill(Ellipse2D.Float(cx - 9.2f * s, cy, 2.8f * s, 7f * s))
        g.fill(Ellipse2D.Float(cx + 6.4f * s, cy, 2.8f * s, 7f * s))
      }
      "rage", "scream" -> g.fill(Ellipse2D.Float(cx - 5.5f * s, cy + 1.8f * s, 11f * s, 8f * s))
      else -> g.draw(Arc2D.Float(cx - 6.5f * s, cy - 0.5f * s, 13f * s, 11f * s, 200f, 140f, Arc2D.OPEN))
    }
  }
}

private fun paintHeart(g: Graphics2D, x: Float, y: Float, size: Float) {
  g.color = Heart
  val path = Path2D.Float()
  val w = size
  val h = size
  path.moveTo(x + w / 2f, y + h * 0.82f)
  path.curveTo(x - w * 0.12f, y + h * 0.48f, x + w * 0.02f, y, x + w / 2f, y + h * 0.28f)
  path.curveTo(x + w * 0.98f, y, x + w * 1.12f, y + h * 0.48f, x + w / 2f, y + h * 0.82f)
  g.fill(path)
}

private fun paintFire(g: Graphics2D, x: Float, y: Float, size: Float) {
  g.color = FireHi
  val path = Path2D.Float()
  path.moveTo(x + size * 0.5f, y)
  path.curveTo(x + size * 0.95f, y + size * 0.35f, x + size * 0.9f, y + size * 0.85f, x + size * 0.5f, y + size)
  path.curveTo(x + size * 0.08f, y + size * 0.78f, x + size * 0.12f, y + size * 0.32f, x + size * 0.5f, y)
  g.fill(path)
  g.color = Face
  g.fill(Ellipse2D.Float(x + size * 0.32f, y + size * 0.42f, size * 0.36f, size * 0.42f))
}

private fun paintThumb(g: Graphics2D, x: Float, y: Float, size: Float, up: Boolean) {
  val saved = g.transform
  if (!up) {
    g.rotate(Math.PI, (x + size / 2f).toDouble(), (y + size / 2f).toDouble())
  }
  g.color = Skin
  g.fill(RoundRectangle2D.Float(x + size * 0.28f, y + size * 0.38f, size * 0.52f, size * 0.48f, 6f, 6f))
  g.fill(RoundRectangle2D.Float(x + size * 0.42f, y + size * 0.08f, size * 0.22f, size * 0.42f, 6f, 6f))
  g.transform = saved
}

private fun paintOkHand(g: Graphics2D, x: Float, y: Float, size: Float) {
  g.color = Skin
  g.fill(Ellipse2D.Float(x + size * 0.12f, y + size * 0.18f, size * 0.76f, size * 0.76f))
  g.color = Panel
  g.fill(Ellipse2D.Float(x + size * 0.32f, y + size * 0.34f, size * 0.28f, size * 0.28f))
}

private fun paintClap(g: Graphics2D, x: Float, y: Float, size: Float) {
  g.color = Skin
  g.fill(RoundRectangle2D.Float(x + size * 0.08f, y + size * 0.22f, size * 0.42f, size * 0.62f, 8f, 8f))
  g.fill(RoundRectangle2D.Float(x + size * 0.48f, y + size * 0.12f, size * 0.42f, size * 0.62f, 8f, 8f))
}

private fun paintPray(g: Graphics2D, x: Float, y: Float, size: Float) {
  g.color = Skin
  g.fill(RoundRectangle2D.Float(x + size * 0.22f, y + size * 0.08f, size * 0.24f, size * 0.82f, 8f, 8f))
  g.fill(RoundRectangle2D.Float(x + size * 0.52f, y + size * 0.08f, size * 0.24f, size * 0.82f, 8f, 8f))
}

private fun paintWave(g: Graphics2D, x: Float, y: Float, size: Float) {
  g.color = Skin
  g.fill(Ellipse2D.Float(x + size * 0.18f, y + size * 0.28f, size * 0.64f, size * 0.64f))
  g.fill(RoundRectangle2D.Float(x + size * 0.18f, y + size * 0.08f, size * 0.16f, size * 0.42f, 6f, 6f))
  g.fill(RoundRectangle2D.Float(x + size * 0.42f, y, size * 0.16f, size * 0.42f, 6f, 6f))
  g.fill(RoundRectangle2D.Float(x + size * 0.66f, y + size * 0.1f, size * 0.16f, size * 0.4f, 6f, 6f))
}

private fun paintMuscle(g: Graphics2D, x: Float, y: Float, size: Float) {
  g.color = Skin
  g.fill(Ellipse2D.Float(x + size * 0.08f, y + size * 0.18f, size * 0.55f, size * 0.55f))
  g.fill(RoundRectangle2D.Float(x + size * 0.48f, y + size * 0.38f, size * 0.42f, size * 0.28f, 8f, 8f))
}

private fun paintShrug(g: Graphics2D, x: Float, y: Float, size: Float) {
  paintFaceGlyph(g, "slightly_smiling", x + size * 0.18f, y + size * 0.08f, size * 0.64f)
  g.color = Skin
  g.stroke = BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
  g.draw(Arc2D.Float(x, y + size * 0.42f, size * 0.32f, size * 0.4f, 40f, 120f, Arc2D.OPEN))
  g.draw(Arc2D.Float(x + size * 0.68f, y + size * 0.42f, size * 0.32f, size * 0.4f, 20f, 120f, Arc2D.OPEN))
}

private fun paintFacepalm(g: Graphics2D, x: Float, y: Float, size: Float) {
  paintFaceGlyph(g, "weary", x, y, size)
  g.color = Skin
  g.fill(RoundRectangle2D.Float(x + size * 0.08f, y + size * 0.28f, size * 0.84f, size * 0.38f, 8f, 8f))
}

private fun noto(): Font {
  val file = listOf(
    File("src/main/res/font/noto_sans_regular.ttf"),
    File("app/src/main/res/font/noto_sans_regular.ttf"),
  ).first { it.isFile }
  return Font.createFont(Font.TRUETYPE_FONT, file)
}
