package com.jetbrains.edu.learning.newproject.ui

import com.intellij.ui.ColorUtil
import com.intellij.util.ui.HtmlPanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import java.awt.Font

class GrayTextHtmlPanel(private val infoText: String, private val style: String = "") : HtmlPanel() {
  init {
    background = MAIN_BG_COLOR
    super.update()
  }

  override fun getBody(): String {
    // this check is needed because this method is called from superclass constructor
    @Suppress("USELESS_ELVIS")
    return infoText ?: ""
  }

  override fun setBody(text: String) {
    if (text.isEmpty()) {
      setText("")
    }
    else {
      setText("""
        <html>
        <head>
          <style>
            body {
            color: #${ColorUtil.toHex(GRAY_COLOR)};
            $style;
            }
          </style>
        </head>
        <body>
        $text
        </body>
        </html>
      """.trimIndent())
    }
  }

  override fun getBodyFont(): Font = font.deriveFont(CoursesDialogFontManager.fontSize)
}
