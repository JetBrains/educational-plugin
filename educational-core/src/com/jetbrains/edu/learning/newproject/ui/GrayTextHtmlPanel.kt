package com.jetbrains.edu.learning.newproject.ui

import com.intellij.ui.ColorUtil
import com.intellij.util.ui.HtmlPanel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import java.awt.Font

private const val FONT_SIZE = 13.0f

class GrayTextHtmlPanel(private val infoText: String, private val linkText: String = "") : HtmlPanel() {
  init {
    background = MAIN_BG_COLOR
    super.update()
  }

  override fun getBody(): String = "<span style='color: #${ColorUtil.toHex(GRAY_COLOR)}'>$infoText</span> $linkText"

  override fun getBodyFont(): Font = font.deriveFont(JBUI.scaleFontSize(FONT_SIZE))
}
