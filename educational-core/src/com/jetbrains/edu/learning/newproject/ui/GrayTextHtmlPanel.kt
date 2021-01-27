package com.jetbrains.edu.learning.newproject.ui

import com.intellij.ui.ColorUtil
import com.intellij.util.ui.HtmlPanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import java.awt.Font

class GrayTextHtmlPanel(private val infoText: String, private val linkText: String = "", private val style: String = "") : HtmlPanel() {
  init {
    background = MAIN_BG_COLOR
    super.update()
  }

  override fun getBody(): String = "<p style='$style'><span style='color: #${ColorUtil.toHex(GRAY_COLOR)}'>$infoText</span> $linkText</p>"

  override fun getBodyFont(): Font = font.deriveFont(CoursesDialogFontManager.fontSize)
}
