package com.jetbrains.edu.learning.taskToolWindow.ui.navigationMap

import com.intellij.util.ui.JBFont
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.ui.EduColors
import java.awt.*
import javax.swing.Icon

class EduTextIcon(private val text: String, private val font: Font = JBFont.label().biggerOn(3f)) : Icon {

  private val height: Int
  private val width: Int

  init {
    val fontMetrics = Toolkit.getDefaultToolkit().getFontMetrics(font)
    height = fontMetrics.height
    width = fontMetrics.stringWidth(text)
  }

  override fun getIconHeight(): Int = height

  override fun getIconWidth(): Int = width

  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    c ?: return
    val g2 = g.create() as Graphics2D
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      val fontMetrics = g2.getFontMetrics(font)
      g2.font = font
      g2.color = if (c.isEnabled) UIUtil.getLabelForeground() else EduColors.navigationMapDisabledIconForeground
      g2.drawString(text, x, y + fontMetrics.maxAscent)
    }
    finally {
      g2.dispose()
    }
  }

}