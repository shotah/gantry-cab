package com.gantree.cab.ui

import com.gantree.cab.dev.sampleScene
import com.gantree.cab.drive.carRows
import com.gantree.cab.mailbox.displaySlug
import com.gantree.cab.mailbox.searchEmoji
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.kordamp.ikonli.Ikon
import org.kordamp.ikonli.fontawesome6.FontAwesomeRegular
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid
import org.kordamp.ikonli.material2.Material2OutlinedAL
import org.kordamp.ikonli.material2.Material2OutlinedMZ
import org.kordamp.ikonli.swing.FontIcon

private val Canvas = Color(0x0E1316)
private val Panel = Color(0x171D22)
private val Track = Color(0x232B32)
private val Body = Color(0xDCD6CE)
private val Fg = Color(0xF4F0EA)
private val Muted = Color(0x9AA3AB)
private val Dim = Color(0x84909A)
private val Accent = Color(0xF07848)
private val Live = Color(0x3DB8A0)
private val You = Color(0x2A1612)
private val Kit = Color(0x232B32)
private val Field = Color(0x3A4550)
private val AccentSoft = Color(0x2A1612)
private val Mark = Color(0xF3B199)
private val Edge = Color(0x5C6772)
private val CarBg = Color(0x121212)
private val CarLine = Color(0x2A2A2A)
private val CarTitle = Color(0xF2F2F2)
private val CarBody = Color(0xB8B8B8)

const val PHONE_W = 390
const val PHONE_H = 844
const val AUTO_W = 1024
const val AUTO_H = 576
private const val BAR_H = 64
private const val COMPOSE_H = 72

val DOCS_SHOT_NAMES = listOf(
  "phone-unsigned",
  "phone-empty",
  "phone-thread",
  "phone-stream",
  "phone-down",
  "phone-settings",
  "phone-emoji",
  "phone-attach",
  "auto-empty",
  "auto-thread",
)

fun renderDocsShot(name: String): BufferedImage = when (name) {
  "phone-unsigned" -> renderPhone("unsigned")
  "phone-empty" -> renderPhone("empty")
  "phone-thread" -> renderPhone("thread")
  "phone-stream" -> renderPhone("stream")
  "phone-down" -> renderPhone("down")
  "phone-settings" -> renderPhone("empty", settings = true)
  "phone-emoji" -> renderPhone("thread", emoji = true)
  "phone-attach" -> renderPhone("thread", attach = true)
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

fun renderPhone(sampleId: String, settings: Boolean = false, emoji: Boolean = false, attach: Boolean = false): BufferedImage {
  val scene = sampleScene(sampleId) ?: error("sample $sampleId")
  val img = BufferedImage(PHONE_W, PHONE_H, BufferedImage.TYPE_INT_ARGB)
  val g = img.graphics2d()
  val font = noto()
  val door = sampleId == "unsigned"
  g.color = Canvas
  g.fillRect(0, 0, PHONE_W, PHONE_H)
  if (settings) {
    paintSettings(g, font, scene.slug, scene.email)
    g.dispose()
    return img
  }
  paintHeader(g, font, scene.slug, scene.up, scene.typing)
  var y = BAR_H + 8
  if (!door && scene.hint.isNotBlank()) {
    g.font = font.deriveFont(12f)
    g.color = Muted
    y = g.drawLine(scene.hint, 16, y, 12)
  }
  val composerTop = PHONE_H - COMPOSE_H
  val overlayH = when {
    emoji -> 128
    attach -> 176
    else -> 0
  }
  val overlayTop = composerTop - 8 - overlayH
  if (door) {
    paintAvatar(g, (PHONE_W - 72) / 2f, 280f, 72f)
    g.font = font.deriveFont(16f)
    g.color = Muted
    val msg = "Sign in with Google to talk."
    g.drawString(msg, (PHONE_W - g.fontMetrics.stringWidth(msg)) / 2, 380)
    val btn = "Continue with Google"
    g.font = font.deriveFont(14f)
    val bw = g.fontMetrics.stringWidth(btn) + 40
    val bx = (PHONE_W - bw) / 2
    g.color = Accent
    g.fill(RoundRectangle2D.Float(bx.toFloat(), 396f, bw.toFloat(), 44f, 24f, 24f))
    g.color = Canvas
    g.drawString(btn, bx + 20, 424)
  } else {
    if (scene.lines.isEmpty()) {
      paintAvatar(g, (PHONE_W - 72) / 2f, 200f, 72f)
      g.font = font.deriveFont(16f)
      g.color = Muted
      val empty = "No messages yet. Say hello, or type / for commands."
      val wrapped = wrap(g, empty, PHONE_W - 80)
      var ty = 296
      for (w in wrapped) {
        g.drawString(w, (PHONE_W - g.fontMetrics.stringWidth(w)) / 2, ty)
        ty += 22
      }
    } else {
      val bubbleMax = PHONE_W - 16 - 48 - 24
      for (line in scene.lines) {
        val draft = line.kind == "draft"
        g.font = if (draft) font.deriveFont(Font.ITALIC, 16f) else font.deriveFont(16f)
        val wrapped = wrap(g, line.text, bubbleMax)
        val bh = 12 + wrapped.size * 20 + 12
        val bw = (wrapped.maxOf { g.fontMetrics.stringWidth(it) } + 28).coerceAtMost(PHONE_W - 64)
        val x = if (line.fromYou) PHONE_W - 16 - bw else 16
        g.color = if (line.fromYou) You else Kit
        g.fill(RoundRectangle2D.Float(x.toFloat(), y.toFloat(), bw.toFloat(), bh.toFloat(), 20f, 20f))
        g.color = when {
          draft -> Dim
          line.fromYou -> Mark
          else -> Fg
        }
        var ty = y + 12
        for (w in wrapped) {
          g.drawString(w, x + 14, ty + g.fontMetrics.ascent)
          ty += 20
        }
        y += bh + 10
        if (y > (if (overlayH > 0) overlayTop else composerTop) - 8) break
      }
    }
    if (emoji) {
      paintEmojiPanel(g, font, overlayTop)
    }
    if (attach) {
      paintAttachMenu(g, font, overlayTop)
    }
    paintComposer(g, font, composerTop, scene.slug, emoji)
  }
  g.dispose()
  return img
}

fun renderAuto(sampleId: String): BufferedImage {
  val scene = sampleScene(sampleId) ?: error("sample $sampleId")
  val rows = carRows(scene.lines, scene.slug, "No messages yet. Reply on this conversation to talk.")
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

private fun paintHeader(g: Graphics2D, font: Font, slug: String, up: Boolean, typing: Boolean = false) {
  g.color = Panel
  g.fillRect(0, 0, PHONE_W, BAR_H)
  paintAvatar(g, 12f, 12f, 40f)
  g.font = font.deriveFont(18f)
  g.color = Fg
  g.drawString(displaySlug(slug), 64, 32)
  g.color = if (up) Live else Dim
  g.fill(Ellipse2D.Float(64f, 42f, 8f, 8f))
  g.font = font.deriveFont(11f)
  val subtitle = when {
    !up -> "Offline"
    typing -> "Live · typing…"
    else -> "Live"
  }
  g.drawString(subtitle, 78, 50)
  paintIkon(g, Material2OutlinedMZ.SETTINGS, PHONE_W - 28f, 32f, 22, Muted)
}

private fun paintAvatar(g: Graphics2D, x: Float, y: Float, size: Float) {
  g.color = Track
  g.fill(Ellipse2D.Float(x, y, size, size))
  paintIkon(
    g,
    Material2OutlinedMZ.SENTIMENT_SATISFIED,
    x + size / 2f,
    y + size / 2f,
    (size * 0.55f).toInt().coerceAtLeast(12),
    Muted,
  )
}

private fun paintSettings(g: Graphics2D, font: Font, slug: String, email: String) {
  g.color = Panel
  g.fillRect(0, 0, PHONE_W, PHONE_H)
  paintIkon(g, Material2OutlinedAL.ARROW_BACK, 28f, 32f, 22, Fg)
  g.font = font.deriveFont(18f)
  g.color = Fg
  g.drawString("Settings", 56, 40)
  val inset = 24
  val fieldW = (PHONE_W - 48).toFloat()
  var y = BAR_H + 16
  y = g.outlinedField(font, "Mailbox", "https://pendant.example.com", inset, y, fieldW)
  y = g.outlinedField(font, "Talking to", slug, inset, y + 8, fieldW)
  y = g.outlinedField(font, "Phone secret", "••••••••", inset, y + 8, fieldW)
  g.font = font.deriveFont(12f)
  g.color = Muted
  y = g.drawLine("Theme", inset, y + 12, 12)
  var x = inset.toFloat()
  for (label in listOf("Boom", "Inlay", "Lamp")) {
    val on = label == "Boom"
    val cw = 88f
    g.color = if (on) Track else Canvas
    g.fill(RoundRectangle2D.Float(x, y.toFloat(), cw, 36f, 8f, 8f))
    g.color = if (on) Accent else Edge
    g.stroke = BasicStroke(1f)
    g.draw(RoundRectangle2D.Float(x, y.toFloat(), cw, 36f, 8f, 8f))
    paintThemeSwatch(g, x + 10f, y + 12f, label.lowercase())
    g.font = font.deriveFont(12f)
    g.color = Fg
    g.drawString(label, x.toInt() + 28, y + 24)
    x += cw + 8f
  }
  y += 48
  g.color = Muted
  g.font = font.deriveFont(12f)
  y = g.drawLine("Font size", inset, y, 12)
  x = inset.toFloat()
  for (label in listOf("Small", "Medium", "Large", "XL")) {
    val on = label == "Small"
    val cw = 78f
    g.color = if (on) Track else Canvas
    g.fill(RoundRectangle2D.Float(x, y.toFloat(), cw, 36f, 8f, 8f))
    g.color = if (on) Accent else Edge
    g.stroke = BasicStroke(1f)
    g.draw(RoundRectangle2D.Float(x, y.toFloat(), cw, 36f, 8f, 8f))
    g.font = font.deriveFont(12f)
    g.color = Fg
    g.drawString(label, x.toInt() + 12, y + 24)
    x += cw + 8f
  }
  y += 52
  g.color = AccentSoft
  g.fill(RoundRectangle2D.Float(inset.toFloat(), y.toFloat(), 108f, 40f, 20f, 20f))
  g.font = font.deriveFont(14f)
  g.color = Mark
  g.drawString("Connect", inset + 22, y + 26)
  if (email.isNotBlank()) {
    g.color = Accent
    g.font = font.deriveFont(14f)
    g.drawString("Sign out", inset + 128, y + 26)
    y += 54
    g.font = font.deriveFont(12f)
    g.color = Muted
    g.drawString(email, inset, y)
  }
}

private fun paintThemeSwatch(g: Graphics2D, x: Float, y: Float, themeId: String) {
  val size = 12f
  val canvas = when (themeId) {
    "inlay" -> Color(0x0C110F)
    "lamp" -> Color(0x0C0C16)
    else -> Canvas
  }
  val accent = when (themeId) {
    "inlay" -> Color(0xE6D3B0)
    "lamp" -> Color(0xC5D24A)
    else -> Accent
  }
  val clip = g.clip
  g.clip = Ellipse2D.Float(x, y, size, size)
  g.color = canvas
  g.fillRect(x.toInt(), y.toInt(), (size / 2).toInt() + 1, size.toInt())
  g.color = accent
  g.fillRect((x + size / 2).toInt(), y.toInt(), (size / 2).toInt() + 1, size.toInt())
  g.clip = clip
}

private fun Graphics2D.outlinedField(font: Font, label: String, value: String, x: Int, y: Int, width: Float): Int {
  color = Canvas
  fill(RoundRectangle2D.Float(x.toFloat(), y.toFloat() + 8f, width, 52f, 4f, 4f))
  color = Edge
  stroke = BasicStroke(1f)
  draw(RoundRectangle2D.Float(x.toFloat(), y.toFloat() + 8f, width, 52f, 4f, 4f))
  this.font = font.deriveFont(11f)
  color = Muted
  drawString(label, x + 12, y + 6)
  this.font = font.deriveFont(14f)
  color = Fg
  drawString(value, x + 12, y + 40)
  return y + 60
}

private fun paintEmojiPanel(g: Graphics2D, font: Font, top: Int) {
  val faces = searchEmoji("").take(16)
  g.color = Track
  g.fill(RoundRectangle2D.Float(12f, top.toFloat(), (PHONE_W - 24).toFloat(), 120f, 12f, 12f))
  g.color = Field
  g.fill(RoundRectangle2D.Float(20f, (top + 8).toFloat(), (PHONE_W - 40).toFloat(), 32f, 18f, 18f))
  g.font = font.deriveFont(12f)
  g.color = Muted
  g.drawString("Search or :shrug:", 32, top + 29)
  var i = 0
  for (row in 0 until 2) {
    for (col in 0 until 8) {
      if (i >= faces.size) break
      val x = 24f + col * 44f
      val ey = top + 50f + row * 34f
      paintEmoji(g, faces[i].name, x, ey, 22)
      i += 1
    }
  }
}

private fun paintAttachMenu(g: Graphics2D, font: Font, top: Int) {
  val w = 196f
  val h = 168f
  val x = 16f
  g.color = Panel
  g.fill(RoundRectangle2D.Float(x, top.toFloat(), w, h, 8f, 8f))
  g.font = font.deriveFont(14f)
  var y = top + 28
  g.color = Fg
  g.drawString("Photo", x.toInt() + 44, y)
  paintIkon(g, Material2OutlinedMZ.PHOTO, x + 24f, y - 6f, 16, Muted)
  y += 40
  g.color = Fg
  g.drawString("Commands", x.toInt() + 44, y)
  paintIkon(g, Material2OutlinedAL.CODE, x + 24f, y - 6f, 16, Muted)
  y += 40
  g.color = Fg
  g.drawString("Location", x.toInt() + 44, y)
  paintIkon(g, Material2OutlinedAL.LOCATION_ON, x + 24f, y - 6f, 16, Muted)
  g.color = Live
  g.fill(RoundRectangle2D.Float(x + 148f, y - 14f, 32f, 18f, 10f, 10f))
  g.color = Canvas
  g.fill(Ellipse2D.Float(x + 164f, y - 12f, 14f, 14f))
  y += 40
  g.color = Fg
  g.drawString("Drop a pin", x.toInt() + 44, y)
  paintIkon(g, Material2OutlinedAL.ADD_LOCATION_ALT, x + 24f, y - 6f, 16, Muted)
}

private fun paintComposer(g: Graphics2D, font: Font, composerTop: Int, slug: String, emojiOpen: Boolean) {
  g.color = Panel
  g.fillRect(0, composerTop, PHONE_W, PHONE_H - composerTop)
  val send = 48
  val sendX = PHONE_W - 12 - send
  val fieldX = 12f
  val fieldW = (sendX - 8 - fieldX.toInt()).toFloat()
  g.color = Track
  g.fill(RoundRectangle2D.Float(fieldX, (composerTop + 12).toFloat(), fieldW, 48f, 28f, 28f))
  val iconY = composerTop + 36f
  paintIkon(g, Material2OutlinedAL.ATTACH_FILE, fieldX + 22f, iconY, 18, Muted)
  g.color = Live
  g.fill(Ellipse2D.Float(fieldX + 28f, composerTop + 16f, 8f, 8f))
  paintIkon(
    g,
    Material2OutlinedMZ.SENTIMENT_SATISFIED,
    fieldX + fieldW - 22f,
    iconY,
    18,
    if (emojiOpen) Accent else Muted,
  )
  g.font = font.deriveFont(14f)
  g.color = Muted
  g.drawString("Message ${displaySlug(slug)}", fieldX.toInt() + 44, composerTop + 42)
  val saved = g.composite
  g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f)
  g.color = Accent
  g.fill(Ellipse2D.Float(sendX.toFloat(), (composerTop + 12).toFloat(), send.toFloat(), send.toFloat()))
  g.composite = saved
  paintIkon(g, Material2OutlinedMZ.SEND, sendX + send / 2f, composerTop + 36f, 20, Canvas)
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
private val Heart = Color(0xE24B4A)
private val FireHi = Color(0xF07848)

private fun paintIkon(g: Graphics2D, ikon: Ikon, cx: Float, cy: Float, size: Int, color: Color) {
  val icon = FontIcon.of(ikon, size, color)
  icon.paintIcon(null, g, (cx - icon.iconWidth / 2f).toInt(), (cy - icon.iconHeight / 2f).toInt())
}

private fun paintEmoji(g: Graphics2D, name: String, x: Float, y: Float, size: Int) {
  paintIkon(g, emojiIkon(name), x + size / 2f, y + size / 2f, size, emojiColor(name))
}

private fun emojiColor(name: String): Color = when (name) {
  "heart" -> Heart
  "fire" -> FireHi
  "rage" -> Color(0xE05A32)
  else -> Face
}

private fun emojiIkon(name: String): Ikon = when (name) {
  "grinning" -> FontAwesomeRegular.GRIN
  "smile" -> FontAwesomeRegular.SMILE
  "joy" -> FontAwesomeRegular.GRIN_TEARS
  "blush" -> FontAwesomeRegular.SMILE_BEAM
  "wink" -> FontAwesomeRegular.SMILE_WINK
  "heart_eyes" -> FontAwesomeRegular.GRIN_HEARTS
  "thinking" -> FontAwesomeRegular.MEH
  "sunglasses" -> FontAwesomeRegular.GRIN_STARS
  "cry" -> FontAwesomeRegular.SAD_TEAR
  "sob" -> FontAwesomeRegular.SAD_CRY
  "rage" -> FontAwesomeRegular.ANGRY
  "scream" -> FontAwesomeRegular.SURPRISE
  "partying" -> FontAwesomeRegular.LAUGH_BEAM
  "shrug" -> FontAwesomeRegular.MEH_BLANK
  "facepalm" -> FontAwesomeRegular.FLUSHED
  "thumbsup" -> FontAwesomeRegular.THUMBS_UP
  "thumbsdown" -> FontAwesomeRegular.THUMBS_DOWN
  "ok_hand" -> FontAwesomeRegular.HAND_PEACE
  "clap" -> FontAwesomeSolid.HANDS
  "pray" -> FontAwesomeSolid.PRAYING_HANDS
  "wave" -> FontAwesomeRegular.HAND_PAPER
  "muscle" -> FontAwesomeRegular.HAND_ROCK
  "heart" -> FontAwesomeRegular.HEART
  "fire" -> FontAwesomeSolid.FIRE
  else -> FontAwesomeRegular.SMILE
}

private fun noto(): Font {
  val file = listOf(
    File("src/main/res/font/noto_sans_regular.ttf"),
    File("app/src/main/res/font/noto_sans_regular.ttf"),
  ).first { it.isFile }
  return Font.createFont(Font.TRUETYPE_FONT, file)
}
