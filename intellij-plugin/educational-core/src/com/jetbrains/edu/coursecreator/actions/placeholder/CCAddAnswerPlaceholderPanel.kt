package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.*

class CCAddAnswerPlaceholderPanel(@NonNls placeholder: AnswerPlaceholder) : JPanel() {
  private val textArea: JTextArea = JTextArea(placeholder.placeholderText, 0, 0)
  private val visibilityCheckBox: JBCheckBox = JBCheckBox(
    EduCoreBundle.message("label.visible"),
    placeholder.isVisible
  )

  init {
    layout = BorderLayout()

    val placeholderHintLabel = JLabel(EduCoreBundle.message("ui.panel.add.answer.placeholder.help"))
    placeholderHintLabel.foreground = JBColor.GRAY
    placeholderHintLabel.border = JBUI.Borders.emptyTop(5)

    val visibilityHintLabel = JLabel(EduCoreBundle.message("ui.panel.add.answer.placeholder.help.visibility"))
    visibilityHintLabel.foreground = JBColor.GRAY

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
    val panel = panel {
      row {
        resizableRow()
        cell(scrollPane)
          .align(Align.FILL)
      }
      row { cell(placeholderHintLabel) }
      row {
        cell(visibilityCheckBox)
        cell(visibilityHintLabel)
      }
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

  fun getVisible(): Boolean = visibilityCheckBox.isSelected

  companion object {
    const val PLACEHOLDER_PANEL_WIDTH = 400
  }
}