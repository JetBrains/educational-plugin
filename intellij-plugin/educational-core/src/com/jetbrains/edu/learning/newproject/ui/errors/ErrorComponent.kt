package com.jetbrains.edu.learning.newproject.ui.errors

import com.intellij.openapi.util.NlsSafe
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.CurrentTheme.Validator.errorBackgroundColor
import com.intellij.util.ui.JBUI.CurrentTheme.Validator.warningBackgroundColor
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseBindData
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseSelectionListener
import com.jetbrains.edu.learning.newproject.ui.coursePanel.SelectCourseBackgroundColor
import com.jetbrains.edu.learning.newproject.ui.createCourseDescriptionStylesheet
import com.jetbrains.edu.learning.newproject.ui.createErrorStylesheet
import com.jetbrains.edu.learning.taskToolWindow.ui.createTextPane
import java.awt.*
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JTextPane
import javax.swing.event.HyperlinkListener


class ErrorComponent(
  hyperlinkListener: HyperlinkListener? = null,
  errorPanelTopBottomMargin: Int = 3,
  errorPanelLeftMargin: Int = 20,
  icon: Icon? = null,
  private val doValidation: (Course?) -> Unit
) : NonOpaquePanel(), CourseSelectionListener {
  private val errorPanel = ErrorPanel(icon, hyperlinkListener, errorPanelTopBottomMargin, errorPanelLeftMargin)
  var validationMessageLink: String? = null
  init {
    isVisible = false
    add(errorPanel)
  }

  fun setErrorMessage(validationMessage: ValidationMessage) {
    errorPanel.setErrorMessage(validationMessage)
  }

  inner class ErrorPanel(icon: Icon?, hyperlinkListener: HyperlinkListener?, topMargin: Int, leftMargin: Int) : NonOpaquePanel() {

    private val errorTextPane: JTextPane = createTextPane()
    private var messageType: ValidationMessageType = ValidationMessageType.INFO
    private val actionButton: JButton = JButton()

    init {
      border = JBUI.Borders.empty(topMargin, leftMargin, topMargin, 0)
      background = SelectCourseBackgroundColor
      layout = BorderLayout()

      if (icon != null) {
        add(JBLabel(icon), BorderLayout.LINE_START)
      }

      errorTextPane.apply {
        background = getComponentColor()
        highlighter = null
        // specifying empty borders is necessary because otherwise JTextPane's preferred size is defined as zero on MAC OS
        border = JBUI.Borders.empty(1, 5, 1, 1)
        addHyperlinkListener(hyperlinkListener)
      }
      add(errorTextPane, BorderLayout.CENTER)

      actionButton.isVisible = false
      actionButton.border = JBUI.Borders.empty(0, 5, 0, 5)
      add(actionButton, BorderLayout.LINE_END)
    }

    fun setErrorMessage(validationMessage: ValidationMessage) {
      @Suppress("UnstableApiUsage")
      @NlsSafe
      val wrappedMessage = """
      <style>
        ${createCourseDescriptionStylesheet()}
        ${createErrorStylesheet()}
      </style>
      <body>
        ${validationMessage.message}
      </body>
      """.trimIndent()

      messageType = validationMessage.type
      errorTextPane.text = wrappedMessage
      errorTextPane.background = getComponentColor()
      validationMessageLink = validationMessage.hyperlinkAddress

      // Handle action button
      // Remove all existing action listeners to avoid duplicates
      for (listener in actionButton.actionListeners) {
        actionButton.removeActionListener(listener)
      }

      if (validationMessage.actionButtonText != null && validationMessage.action != null) {
        actionButton.text = validationMessage.actionButtonText
        actionButton.addActionListener { validationMessage.action?.invoke() }
        actionButton.isVisible = true
      } else {
        actionButton.isVisible = false
      }
    }

    private fun getComponentColor(): Color {
      return when (messageType) {
        ValidationMessageType.WARNING -> warningBackgroundColor()
        ValidationMessageType.ERROR -> errorBackgroundColor()
        ValidationMessageType.INFO -> JBColor(Color(0xE6EEF7), Color(0x1C3956))
      }
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

  override fun onCourseSelectionChanged(data: CourseBindData) {
    doValidation(data.course)
  }
}
