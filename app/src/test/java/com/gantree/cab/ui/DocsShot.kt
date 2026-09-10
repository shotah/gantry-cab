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
private val You = Color(0x3A1E16)
private val Kit = Color(0x232B32)
private val Field = Color(0x3A4550)
private val AccentLine = Color(0xC24A28)
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

val DOCS_SHOT_NAMES = listOf(
  "phone-unsigned",
  "phone-empty",
  "phone-thread",
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
  paintHeader(g, font, scene.slug, scene.up, settings)
  var y = 64
  if (!door && scene.hint.isNotBlank()) {
    g.font = font.deriveFont(12f)
    g.color = Muted
    y = g.drawLine(scene.hint, 16, y + 4, 12)
  }
  val fieldH = 52
  val pad = 12
  val composerTop = PHONE_H - pad - fieldH
  val overlayH = when {
    emoji -> 176
    attach -> 172
    else -> 0
  }
  val overlayTop = composerTop - 8 - overlayH
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
    g.color = AccentLine
    g.stroke = BasicStroke(1.2f)
    g.draw(RoundRectangle2D.Float(bx.toFloat(), 386f, bw.toFloat(), 40f, 12f, 12f))
    g.color = Mark
    g.drawString(btn, bx + 16, 412)
  } else {
    if (scene.lines.isEmpty()) {
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
        g.color = if (line.fromYou) AccentLine else Field
        g.stroke = BasicStroke(1f)
        g.draw(RoundRectangle2D.Float(x.toFloat(), y.toFloat(), bw.toFloat(), bh.toFloat(), 24f, 24f))
        g.color = Fg
        var ty = y + 12
        for (w in wrapped) {
          g.drawString(w, x + 12, ty + g.fontMetrics.ascent)
          ty += 20
        }
        y += bh + 8
        if (y > (if (overlayH > 0) overlayTop else composerTop) - 8) break
      }
    }
    if (settings) {
      paintSettings(g, font, scene.slug, scene.email)
    }
    if (emoji) {
      paintEmojiPanel(g, font, overlayTop)
    }
    if (attach) {
      paintAttachPanel(g, font, overlayTop, scene.hint)
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
  g.color = Field
  g.stroke = BasicStroke(1f)
  g.drawLine(0, 56, PHONE_W, 56)
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

private fun paintCog(g: Graphics2D, cx: Float, cy: Float, color: Color) {
  paintIkon(g, Material2OutlinedMZ.SETTINGS, cx, cy, 18, color)
}

private fun paintSettings(g: Graphics2D, font: Font, slug: String, email: String) {
  val cardW = 256f
  val cardX = PHONE_W - 12f - cardW
  val cardY = 64f
  val cardH = 392f
  g.color = Panel
  g.fill(RoundRectangle2D.Float(cardX, cardY, cardW, cardH, 12f, 12f))
  g.color = Field
  g.stroke = BasicStroke(1.2f)
  g.draw(RoundRectangle2D.Float(cardX, cardY, cardW, cardH, 12f, 12f))
  val inset = cardX.toInt() + 12
  val fieldW = cardW - 24f
  var y = cardY.toInt() + 10
  y = g.field(font, "Mailbox origin", "http://10.0.2.2:3000", inset, y, fieldW)
  y = g.field(font, "Agent name", slug, inset, y + 6, fieldW)
  y = g.field(font, "Agent access secret", "••••••••", inset, y + 6, fieldW)
  g.font = font.deriveFont(12f)
  g.color = Muted
  y = g.drawLine("Theme", inset, y + 8, 12)
  val chipW = (fieldW - 12f) / 3f
  var x = inset.toFloat()
  for (label in listOf("Boom", "Inlay", "Lamp")) {
    val on = label == "Boom"
    g.color = if (on) Track else Canvas
    g.fill(RoundRectangle2D.Float(x, y.toFloat(), chipW, 32f, 8f, 8f))
    g.color = if (on) AccentLine else Edge
    g.stroke = BasicStroke(1.2f)
    g.draw(RoundRectangle2D.Float(x, y.toFloat(), chipW, 32f, 8f, 8f))
    paintThemeSwatch(g, x + 8f, y + 10f, label.lowercase())
    g.font = font.deriveFont(11f)
    g.color = Body
    g.drawString(label, x.toInt() + 24, y + 21)
    x += chipW + 6f
  }
  y += 40
  g.color = Muted
  g.font = font.deriveFont(12f)
  y = g.drawLine("Font size", inset, y, 12)
  val fontW = (fieldW - 18f) / 4f
  x = inset.toFloat()
  for (size in listOf(12f, 14f, 16f, 18f)) {
    val on = size == 12f
    g.color = if (on) Track else Canvas
    g.fill(RoundRectangle2D.Float(x, y.toFloat(), fontW, 32f, 8f, 8f))
    g.color = if (on) AccentLine else Edge
    g.stroke = BasicStroke(1.2f)
    g.draw(RoundRectangle2D.Float(x, y.toFloat(), fontW, 32f, 8f, 8f))
    g.font = font.deriveFont(size)
    g.color = Fg
    val aa = "Aa"
    g.drawString(aa, x.toInt() + ((fontW - g.fontMetrics.stringWidth(aa)) / 2).toInt(), y + 22)
    x += fontW + 6f
  }
  y += 44
  g.color = Field
  g.stroke = BasicStroke(1f)
  g.drawLine(inset, y, (inset + fieldW).toInt(), y)
  y += 10
  val listenH = 32f
  g.color = AccentSoft
  g.fill(RoundRectangle2D.Float(inset.toFloat(), y.toFloat(), 72f, listenH, 12f, 12f))
  g.color = AccentLine
  g.stroke = BasicStroke(1.2f)
  g.draw(RoundRectangle2D.Float(inset.toFloat(), y.toFloat(), 72f, listenH, 12f, 12f))
  g.font = font.deriveFont(14f)
  g.color = Mark
  g.drawString("Listen", inset + 16, y + 21)
  if (email.isNotBlank()) {
    g.font = font.deriveFont(12f)
    g.color = Dim
    g.drawString("sign out", inset + 84, y + 21)
  }
  y += listenH.toInt() + 14
  if (email.isNotBlank()) {
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
  g.color = Field
  g.stroke = BasicStroke(1f)
  g.draw(Ellipse2D.Float(x, y, size, size))
}

private fun Graphics2D.field(font: Font, label: String, value: String, x: Int, y: Int, width: Float = (PHONE_W - 32).toFloat()): Int {
  this.font = font.deriveFont(11f)
  color = Muted
  val afterLabel = drawLine(label, x, y, 11)
  color = Canvas
  fill(RoundRectangle2D.Float(x.toFloat(), afterLabel.toFloat(), width, 32f, 8f, 8f))
  color = Edge
  stroke = BasicStroke(1.2f)
  draw(RoundRectangle2D.Float(x.toFloat(), afterLabel.toFloat(), width, 32f, 8f, 8f))
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
  g.color = Edge
  g.draw(RoundRectangle2D.Float(22f, (top + 10).toFloat(), (PHONE_W - 44).toFloat(), 30f, 8f, 8f))
  g.font = font.deriveFont(13f)
  g.color = Muted
  g.drawString("Search or :shrug:", 32, top + 30)
  var i = 0
  for (row in 0 until 3) {
    for (col in 0 until 8) {
      if (i >= faces.size) break
      val x = 26f + col * 44f
      val ey = top + 58f + row * 36f
      paintEmoji(g, faces[i].name, x, ey, 24)
      i += 1
    }
  }
}

private fun paintAttachPanel(g: Graphics2D, font: Font, top: Int, hint: String) {
  val w = 208f
  val h = 164f
  val x = 16f
  g.color = Panel
  g.fill(RoundRectangle2D.Float(x, top.toFloat(), w, h, 12f, 12f))
  g.color = Field
  g.stroke = BasicStroke(1.2f)
  g.draw(RoundRectangle2D.Float(x, top.toFloat(), w, h, 12f, 12f))
  g.font = font.deriveFont(12f)
  var y = top + 18
  g.color = Body
  g.drawString("Photo", x.toInt() + 14, y)
  y += 28
  g.drawString("Commands", x.toInt() + 14, y)
  y += 28
  if (hint.isNotBlank()) {
    g.font = font.deriveFont(11f)
    g.color = Dim
    g.drawString(hint, x.toInt() + 14, y)
    y += 24
  }
  g.font = font.deriveFont(12f)
  g.color = Body
  g.drawString("GPS", x.toInt() + 14, y)
  g.color = Live
  g.drawString("on", x.toInt() + 168, y)
  y += 28
  g.color = Body
  g.drawString("Drop a silent pin", x.toInt() + 14, y)
}

private fun paintComposer(g: Graphics2D, font: Font, composerTop: Int, slug: String, emojiOpen: Boolean) {
  g.color = Panel
  g.fillRect(0, composerTop - 12, PHONE_W, PHONE_H - composerTop + 12)
  g.color = Field
  g.stroke = BasicStroke(1f)
  g.drawLine(0, composerTop - 12, PHONE_W, composerTop - 12)
  val sendW = 68
  val pad = 12
  val gap = 8
  val fieldX = pad.toFloat()
  val fieldW = (PHONE_W - pad - gap - sendW - pad).toFloat()
  val sendX = PHONE_W - pad - sendW
  g.color = Canvas
  g.fill(RoundRectangle2D.Float(fieldX, composerTop.toFloat(), fieldW, 52f, 12f, 12f))
  g.color = Edge
  g.stroke = BasicStroke(1.2f)
  g.draw(RoundRectangle2D.Float(fieldX, composerTop.toFloat(), fieldW, 52f, 12f, 12f))
  paintIkon(
    g,
    Material2OutlinedMZ.SENTIMENT_SATISFIED,
    30f,
    composerTop + 14f,
    16,
    if (emojiOpen) Mark else Muted,
  )
  paintIkon(g, Material2OutlinedAL.ATTACH_FILE, 30f, composerTop + 36f, 14, Muted)
  g.color = Live
  g.fill(Ellipse2D.Float(33f, composerTop + 30f, 7f, 7f))
  g.font = font.deriveFont(14f)
  g.color = Muted
  g.drawString("Message ${displaySlug(slug)}", 48, composerTop + 32)
  val saved = g.composite
  g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f)
  g.color = AccentSoft
  g.fill(RoundRectangle2D.Float(sendX.toFloat(), composerTop.toFloat(), sendW.toFloat(), 52f, 12f, 12f))
  g.color = AccentLine
  g.draw(RoundRectangle2D.Float(sendX.toFloat(), composerTop.toFloat(), sendW.toFloat(), 52f, 12f, 12f))
  g.font = font.deriveFont(14f)
  g.color = Mark
  val send = "Send"
  g.drawString(send, sendX + (sendW - g.fontMetrics.stringWidth(send)) / 2, composerTop + 32)
  g.composite = saved
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
