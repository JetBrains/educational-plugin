package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.jetbrains.edu.learning.ui.EduColors
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel


class LessonHeader : JPanel() {
  private val headerText: JLabel

  init {
    layout = BoxLayout(this, BoxLayout.X_AXIS)
    border = JBEmptyBorder(12, 0, 12, 0)

    headerText = JBLabel().withFont(JBFont.medium())
    headerText.foreground = EduColors.taskToolWindowLessonLabel
    headerText.alignmentX = LEFT_ALIGNMENT

    val leftBox = Box.createHorizontalBox()
    leftBox.add(headerText)
    leftBox.add(Box.createHorizontalGlue())
    add(leftBox)
  }

  fun setHeaderText(header: String?) {
    headerText.text = header
  }
}