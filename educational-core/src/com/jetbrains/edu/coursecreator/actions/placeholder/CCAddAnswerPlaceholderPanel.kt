package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.*

class CCAddAnswerPlaceholderPanel(@NonNls placeholderText: String) : JPanel() {
  private val panel: JPanel
  private val textArea: JTextArea = JTextArea(placeholderText, 0, 0)

  init {
    layout = BorderLayout()

    val label = JLabel(EduCoreBundle.message("ui.panel.add.answer.placeholder.help"))
    label.foreground = JBColor.GRAY
    label.border = JBUI.Borders.emptyTop(5)

    textArea.minimumSize = JBUI.size(PLACEHOLDER_PANEL_WIDTH, 60)
    textArea.addFocusListener(object : FocusAdapter() {
      override fun focusGained(e: FocusEvent?) {
        textArea.selectAll()
      }
    })
    textArea.font = UIUtil.getLabelFont()
    textArea.lineWrap = true
    textArea.wrapStyleWord = true

    val scrollPane = JBScrollPane(textArea)
    scrollPane.border = BorderFactory.createLineBorder(JBColor.border())
    scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    panel = panel {
      row { scrollPane() }
      row { label() }
    }
    panel.minimumSize = JBUI.size(PLACEHOLDER_PANEL_WIDTH, 100)
    panel.alignmentX = Component.LEFT_ALIGNMENT
    add(panel, BorderLayout.CENTER)
  }

  fun getAnswerPlaceholderText(): String {
    return textArea.text
  }

  fun getPreferredFocusedComponent(): JComponent {
    return textArea
  }

  companion object {
    const val PLACEHOLDER_PANEL_WIDTH = 400
  }
}