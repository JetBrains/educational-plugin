@file:JvmName("SwingTaskUtil")

package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.CheckStatus
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

private fun ChoiceTask.createSpecificPanel(): JPanel =
  NonOpaquePanel(VerticalFlowLayout())
    .apply {
      border = JBUI.Borders.empty(TOP_INSET, LEFT_INSET, BOTTOM_INSET, RIGHT_INSET)
    }
    .addBox(this, isMultipleChoice)


private fun NonOpaquePanel.addBox(task: ChoiceTask, isMultipleChoice: Boolean): NonOpaquePanel {
  if (isMultipleChoice) {
    addSpecificBox(task) { JCheckBox(it) }
  }
  else {
    addSpecificBox(task, ButtonGroup()) { JRadioButton(it) }
  }
  return this
}

private fun <Button : JToggleButton> NonOpaquePanel.addSpecificBox(
  task: ChoiceTask,
  group: ButtonGroup? = null,
  createBox: (str: String) -> Button
) {
  createTopicForSpecificPanel(task.quizHeader)
  val isEnabled = task.status != CheckStatus.Failed
  for ((index, option) in task.choiceOptions.withIndex()) {
    val box = createBox(option.text).createButton(index, task, enabled = isEnabled, opaque = false, group)
    add(box)
  }
}

private fun NonOpaquePanel.createTopicForSpecificPanel(message: String) {
  val text = JLabel(message, SwingConstants.LEFT).apply { isOpaque = false }
  add(text)
}

private fun <Button : JToggleButton> Button.createButton(
  index: Int,
  task: ChoiceTask,
  enabled: Boolean,
  opaque: Boolean,
  group: ButtonGroup? = null
): Button {
  isOpaque = opaque
  isSelected = task.selectedVariants.contains(index)
  addItemListener(createListener(task, index))
  isEnabled = enabled
  group?.add(this)
  return this
}

private fun createListener(task: ChoiceTask, index: Int): ItemListener {
  return ItemListener {
    if (it.stateChange == ItemEvent.SELECTED) {
      task.selectedVariants.add(index)
    }
    else {
      task.selectedVariants.remove(index)
    }
  }
}

fun createTextPane(editorKit: HTMLEditorKit = UIUtil.JBWordWrapHtmlEditorKit()): JTextPane {
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