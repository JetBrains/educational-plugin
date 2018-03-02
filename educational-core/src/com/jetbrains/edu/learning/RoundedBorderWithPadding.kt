package com.jetbrains.edu.learning

import com.intellij.ui.ColorUtil
import com.intellij.ui.RoundedLineBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.geom.Area
import java.awt.geom.RoundRectangle2D

class RoundedBorderWithPadding(private val arcSize: Int,
                               private val fillInside: Boolean,
                               color: Color,
                               private val backgroundColor: Color = UIUtil.getPanelBackground()) : RoundedLineBorder(color, arcSize, JBUI.scale(1)) {
  private val TEXT_PADDING: Int = 6

  override fun getBorderInsets(c: Component?, insets: Insets?) : Insets = JBUI.insets(TEXT_PADDING)

  override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
    g.color = backgroundColor
    if (fillInside) {
      g.color = ColorUtil.withAlpha(getLineColor(), 0.3)
      g.fillRoundRect(x, y, width - 1, height - 1, arcSize, arcSize)
    } else {
      val area = Area(
        RoundRectangle2D.Double(x.toDouble(), y.toDouble(), (width - 1).toDouble(), (height - 1).toDouble(), arcSize.toDouble(), arcSize.toDouble()))
      area.subtract(Area(Rectangle(x + TEXT_PADDING, y + TEXT_PADDING, width - 2 * TEXT_PADDING, height - 2 * TEXT_PADDING)))
      (g as Graphics2D).fill(area)
    }
    super.paintBorder(c, g, x, y, width, height)
  }
}