package com.gantree.cab.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

@Composable
fun ChatMarkdown(text: String, draft: Boolean, modifier: Modifier = Modifier) {
  val scheme = MaterialTheme.colorScheme
  val body = MaterialTheme.typography.bodyLarge
  val color = if (draft) scheme.onSurfaceVariant else LocalContentColor.current
  val style = body.copy(
    color = color,
    fontStyle = if (draft) FontStyle.Italic else FontStyle.Normal,
  )
  val heading = style.copy(fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
  Markdown(
    content = text,
    colors = markdownColor(text = color),
    typography = markdownTypography(
      h1 = heading.copy(fontSize = style.fontSize * 1.15f),
      h2 = heading.copy(fontSize = style.fontSize * 1.05f),
      h3 = heading,
      h4 = heading,
      h5 = heading,
      h6 = heading,
      text = style,
      code = style.copy(fontFamily = FontFamily.Monospace, fontSize = style.fontSize * 0.85f),
      inlineCode = style.copy(fontFamily = FontFamily.Monospace, fontSize = style.fontSize * 0.85f),
      quote = style.copy(fontStyle = FontStyle.Italic),
      paragraph = style,
      ordered = style,
      bullet = style,
      list = style,
      textLink = TextLinkStyles(
        style = SpanStyle(color = scheme.primary, textDecoration = TextDecoration.Underline),
      ),
      table = style,
    ),
    modifier = modifier.fillMaxWidth(),
  )
}
