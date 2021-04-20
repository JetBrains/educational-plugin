package com.jetbrains.edu.learning.newproject.ui

import com.intellij.ui.ColorUtil
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.CurrentTheme.Validator.errorBackgroundColor
import com.intellij.util.ui.JBUI.CurrentTheme.Validator.warningBackgroundColor
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseDisplaySettings
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseSelectionListener
import com.jetbrains.edu.learning.taskDescription.ui.createTextPane
import com.jetbrains.edu.learning.ui.EduColors
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JTextPane
import javax.swing.event.HyperlinkListener


class ErrorComponent(
  hyperlinkListener: HyperlinkListener? = null,
  errorPanelMargin: Int = 3,
  private val doValidation: () -> Unit
) : NonOpaquePanel(), CourseSelectionListener {
  private val errorPanel = ErrorPanel(hyperlinkListener, errorPanelMargin)

  init {
    isVisible = false
    add(errorPanel)
  }

  fun setErrorMessage(validationMessage: ValidationMessage) {
    errorPanel.setErrorMessage(validationMessage)
  }

  inner class ErrorPanel(hyperlinkListener: HyperlinkListener?, margin: Int) : NonOpaquePanel() {

    private val errorTextPane: JTextPane = createTextPane()
    private var messageType: ValidationMessageType? = null

    init {
      border = JBUI.Borders.empty(margin, 20, margin, 0)
      background = MAIN_BG_COLOR

      errorTextPane.apply {
        background = getComponentColor()
        highlighter = null
        // specifying empty borders is necessary because otherwise JTextPane's preferred size is defined as zero on MAC OS
        border = JBUI.Borders.empty(1)
        addHyperlinkListener(hyperlinkListener)
      }
      add(errorTextPane)
    }

    fun setErrorMessage(validationMessage: ValidationMessage) {
      val text = """
        <style>
          ${createCourseDescriptionStylesheet()}
        </style>
        <body>
          <span>${validationMessage.beforeLink}</span>
          <a style=color:#${ColorUtil.toHex(EduColors.hyperlinkColor)} href=>${validationMessage.linkText}</a>
          <span>${validationMessage.afterLink}</span>
        </body>
      """.trimIndent()
      messageType = validationMessage.type
      errorTextPane.text = text
      errorTextPane.background = getComponentColor()
    }

    private fun getComponentColor(): Color {
      return if (messageType == ValidationMessageType.WARNING) warningBackgroundColor() else errorBackgroundColor()
    }

    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      val arcs = JBUI.size(8, 8)

      val graphics = g as Graphics2D
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

      graphics.color = getComponentColor()
      graphics.fillRoundRect(0, 0, width, height, arcs.width, arcs.height) //paint background

      graphics.color = getComponentColor()
      graphics.drawRoundRect(0, 0, width, height, arcs.width, arcs.height) //paint border
    }
  }

  override fun onCourseSelectionChanged(courseInfo: CourseInfo, courseDisplaySettings: CourseDisplaySettings) {
   doValidation()
  }
}