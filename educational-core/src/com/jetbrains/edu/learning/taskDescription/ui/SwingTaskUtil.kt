@file:JvmName("SwingTaskUtil")

package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.*
import javax.swing.text.html.HTMLEditorKit

private const val LEFT_INSET = 0
private const val RIGHT_INSET = 10
private const val TOP_INSET = 15
private const val BOTTOM_INSET = 10


fun Task?.createSpecificPanel(): JPanel? {
  val choiceTask = this as? ChoiceTask ?: return null
  return choiceTask.createSpecificPanel()
}

fun ChoiceTask.createSpecificPanel(): JPanel {
  val jPanel = NonOpaquePanel(VerticalFlowLayout())
  jPanel.border = JBUI.Borders.empty(TOP_INSET, LEFT_INSET, BOTTOM_INSET, RIGHT_INSET)

  if (this.isMultipleChoice) {
    val text = JLabel(MULTIPLE_CHOICE_LABEL, SwingConstants.LEFT).apply {
      isOpaque = false
    }
    jPanel.add(text)

    for ((index, option) in this.choiceOptions.withIndex()) {
      val checkBox = createCheckBox(option.text, index, this)
      jPanel.add(checkBox)
    }
  }
  else {
    val text = JLabel(SINGLE_CHOICE_LABEL, SwingConstants.LEFT).apply {
      isOpaque = false
    }
    jPanel.add(text)

    val group = ButtonGroup()
    for ((index, option) in this.choiceOptions.withIndex()) {
      val checkBox = createRadioButton(option.text, index, group, this)
      jPanel.add(checkBox)
    }
  }
  return jPanel
}

fun createCheckBox(variant: String?, index: Int, task: ChoiceTask): JCheckBox {
  val checkBox = JCheckBox(variant).apply { isOpaque = false }
  checkBox.isSelected = task.selectedVariants.contains(index)
  checkBox.addItemListener(createListener(task, index))
  return checkBox
}

fun createRadioButton(variant: String, index: Int, group: ButtonGroup, task: ChoiceTask): JRadioButton {
  val button = JRadioButton(variant).apply { isOpaque = false }
  button.isSelected = task.selectedVariants.contains(index)
  button.addItemListener(createListener(task, index))
  group.add(button)
  return button
}

fun createListener(task: ChoiceTask, index: Int): ItemListener? {
  return ItemListener {
    if (it.stateChange == ItemEvent.SELECTED) {
      task.selectedVariants.add(index)
    }
    else {
      task.selectedVariants.remove(index)
    }
  }
}

fun createTextPane(): JTextPane {
  val editorKit = HTMLEditorKit()
  editorKit.styleSheet = null
  prepareCss(editorKit)

  val textPane = object : JTextPane() {
    override fun getSelectedText(): String {
      // see EDU-3185
      return super.getSelectedText().replace(Typography.nbsp, ' ')
    }
  }

  textPane.contentType = editorKit.contentType
  textPane.editorKit = editorKit
  textPane.isEditable = false
  textPane.background = TaskDescriptionView.getTaskDescriptionBackgroundColor()

  return textPane
}

private fun prepareCss(editorKit: HTMLEditorKit) {
  // ul padding of JBHtmlEditorKit is too small, so copy-pasted the style from
  // com.intellij.codeInsight.documentation.DocumentationComponent.prepareCSS
  editorKit.styleSheet.addRule("ul { padding: 3px 16px 0 0; }")
  editorKit.styleSheet.addRule("li { padding: 3px 0 4px 5px; }")
  editorKit.styleSheet.addRule(".hint { padding: 17px 0 16px 0; }")
}