package com.jetbrains.edu.learning.taskToolWindow.ui.navigationMap

import com.intellij.util.text.StringTokenizer
import com.intellij.util.ui.NamedColorUtil
import com.intellij.util.ui.StartupUiUtil
import com.intellij.util.ui.UIUtil
import java.awt.Component
import java.awt.Font
import java.awt.Graphics
import java.awt.Toolkit
import javax.swing.Icon
import kotlin.math.max

class LabeledIcon(private val myIcon: Icon, text: String?, private val myMnemonic: String?) : Icon {
  private val myStrings: Array<String?>?
  var iconTextGap = 5
  var font: Font = StartupUiUtil.labelFont

  init {
    if (text != null) {
      val tokenizer = StringTokenizer(text, "\n")
      myStrings = arrayOfNulls(tokenizer.countTokens())
      var i = 0
      while (tokenizer.hasMoreTokens()) {
        myStrings[i] = tokenizer.nextToken()
        i++
      }
    }
    else {
      myStrings = null
    }
  }

  override fun getIconHeight(): Int {
    return myIcon.iconHeight + textHeight + iconTextGap
  }

  override fun getIconWidth(): Int {
    return max(myIcon.iconWidth.toDouble(), textWidth.toDouble()).toInt()
  }

  private val textHeight: Int
    get() = if (myStrings != null) {
      getFontHeight(myStrings, font)
    }
    else {
      0
    }
  private val textWidth: Int
    get() {
      return if (myStrings != null) {
        var width = 0
        val font: Font = StartupUiUtil.labelFont
        val fontMetrics = Toolkit.getDefaultToolkit().getFontMetrics(font)
        for (string: String? in myStrings) {
          width = fontMetrics.stringWidth(string)
        }
        if (myMnemonic != null) {
          width += fontMetrics.stringWidth(myMnemonic)
        }
        width
      }
      else {
        0
      }
    }

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    // Draw icon
    var y = y
    var width = iconWidth
    val iconWidth = myIcon.iconWidth
    if (width > iconWidth) {
      myIcon.paintIcon(c, g, x + (width - iconWidth) / 2, y)
    }
    else {
      myIcon.paintIcon(c, g, x, y)
    }
    // Draw text
    if (myStrings != null) {
      val font = font
      val fontMetrics = Toolkit.getDefaultToolkit().getFontMetrics(font)
      g.font = fontMetrics.font
      if (myMnemonic != null) {
        width -= fontMetrics.stringWidth(myMnemonic)
      }
      g.color = UIUtil.getLabelForeground()
      y += myIcon.iconHeight + fontMetrics.maxAscent + iconTextGap
      for (string in myStrings) {
        g.drawString(string, x + (width - fontMetrics.stringWidth(string)) / 2, y)
        y += fontMetrics.height
      }
      if (myMnemonic != null) {
        y -= fontMetrics.height
        g.color = NamedColorUtil.getInactiveTextColor()
        val offset = textWidth - fontMetrics.stringWidth(myMnemonic)
        g.drawString(myMnemonic, x + offset, y)
      }
    }
  }

  companion object {
    private fun getFontHeight(strings: Array<String?>, font: Font): Int {
      val fontMetrics = Toolkit.getDefaultToolkit().getFontMetrics(font)
      return fontMetrics.height * strings.size
    }
  }
}
