package com.jetbrains.edu.learning.newproject.ui

import com.intellij.ui.ColorUtil
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.CurrentTheme.Validator.errorBackgroundColor
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import com.jetbrains.edu.learning.taskDescription.ui.createTextPane
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import com.jetbrains.edu.learning.ui.EduColors
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JTextPane
import javax.swing.event.HyperlinkListener


class ErrorComponent(
  hyperlinkListener: HyperlinkListener? = null,
  errorPanelMargin: Int = 3
) : JBScrollPane(VERTICAL_SCROLLBAR_NEVER, HORIZONTAL_SCROLLBAR_NEVER) {

  private val errorPanel = ErrorPanel(hyperlinkListener, errorPanelMargin)

  init {
    isOpaque = false
    setViewportView(errorPanel)
  }

  fun setErrorMessage(beforeLinkText: String, linkText: String, afterLinkText: String) {
    errorPanel.setErrorMessage(beforeLinkText, linkText, afterLinkText)
  }

  inner class ErrorPanel(hyperlinkListener: HyperlinkListener?, margin: Int) : NonOpaquePanel() {

    private val errorTextPane: JTextPane = createTextPane()

    init {
      border = JBUI.Borders.empty(margin, 20, margin, 0)
      background = MAIN_BG_COLOR

      errorTextPane.apply {
        background = errorBackgroundColor()
        highlighter = null
        // specifying empty borders is necessary because otherwise JTextPane's preferred size is defined as zero on MAC OS
        border = JBUI.Borders.empty(1)
        addHyperlinkListener(hyperlinkListener)
      }
      add(errorTextPane)
    }

    fun setErrorMessage(beforeLinkText: String, linkText: String, afterLinkText: String) {
      val text = "<span ${StyleManager().textStyleHeader}>$beforeLinkText</span>" +
                 "<a ${StyleManager().textStyleHeader};color:#${ColorUtil.toHex(EduColors.hyperlinkColor)} href=>${linkText}</a>" +
                 "<span ${StyleManager().textStyleHeader}>$afterLinkText</span>"
      errorTextPane.text = text
    }

    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      val arcs = JBUI.size(8, 8)

      val graphics = g as Graphics2D
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

      graphics.color = errorBackgroundColor()
      graphics.fillRoundRect(0, 0, width, height, arcs.width, arcs.height) //paint background

      graphics.color = errorBackgroundColor()
      graphics.drawRoundRect(0, 0, width, height, arcs.width, arcs.height) //paint border
    }
  }
}