@file:JvmName("SwingTaskUtil")

package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
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
  val jPanel = JPanel(VerticalFlowLayout())
  jPanel.border = JBUI.Borders.empty(com.jetbrains.edu.learning.taskDescription.ui.TOP_INSET,
                                     com.jetbrains.edu.learning.taskDescription.ui.LEFT_INSET,
                                     com.jetbrains.edu.learning.taskDescription.ui.BOTTOM_INSET,
                                     com.jetbrains.edu.learning.taskDescription.ui.RIGHT_INSET)

  if (this.isMultipleChoice) {
    val text = JLabel(MULTIPLE_CHOICE_LABEL, SwingConstants.LEFT)
    jPanel.add(text)

    for ((index, option) in this.choiceOptions.withIndex()) {
      val checkBox = com.jetbrains.edu.learning.taskDescription.ui.createCheckBox(option.text, index, this)
      jPanel.add(checkBox)
    }
  }
  else {
    val text = JLabel(SINGLE_CHOICE_LABEL, SwingConstants.LEFT)
    jPanel.add(text)

    val group = ButtonGroup()
    for ((index, option) in this.choiceOptions.withIndex()) {
      val checkBox = com.jetbrains.edu.learning.taskDescription.ui.createRadioButton(option.text, index, group, this)
      jPanel.add(checkBox)
    }
  }

  return jPanel
}

fun createCheckBox(variant: String?, index: Int, task: ChoiceTask): JCheckBox {
  val checkBox = JCheckBox(variant)
  checkBox.isSelected = task.selectedVariants.contains(index)
  checkBox.addItemListener(com.jetbrains.edu.learning.taskDescription.ui.createListener(task, index))
  return checkBox
}

fun createRadioButton(variant: String, index: Int, group: ButtonGroup, task: ChoiceTask): JRadioButton {
  val button = JRadioButton(variant)
  button.isSelected = task.selectedVariants.contains(index)
  button.addItemListener(com.jetbrains.edu.learning.taskDescription.ui.createListener(task, index))
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
  val editorKit = UIUtil.JBWordWrapHtmlEditorKit()
  com.jetbrains.edu.learning.taskDescription.ui.prepareCss(editorKit)

  val textPane = JTextPane()
  textPane.contentType = editorKit.contentType
  textPane.editorKit = editorKit
  textPane.isEditable = false
  textPane.background = TaskDescriptionView.Companion.getTaskDescriptionBackgroundColor()

  return textPane
}

private fun prepareCss(editorKit: HTMLEditorKit) {
  // ul padding of JBHtmlEditorKit is too small, so copy-pasted the style from
  // com.intellij.codeInsight.documentation.DocumentationComponent.prepareCSS
  editorKit.styleSheet.addRule("ul { padding: 5px 16px 0 7px; }")
  editorKit.styleSheet.addRule("li { padding: 1px 0 2px 0; }")
}