@file:JvmName("SwingTaskSpecificPanel")

package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.tasks.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.*

private const val LEFT_INSET = 15
private const val RIGHT_INSET = 10
private const val TOP_INSET = 15
private const val BOTTOM_INSET = 10


fun Task?.createSpecificPanel(): JPanel? {
  val choiceTask = this as? ChoiceTask ?: return null
  return choiceTask.createSpecificPanel()
}

fun ChoiceTask.createSpecificPanel(): JPanel {
  val jPanel = JPanel(VerticalFlowLayout())
  jPanel.border = JBUI.Borders.empty(TOP_INSET, LEFT_INSET, BOTTOM_INSET, RIGHT_INSET)

  if (this.isMultipleChoice) {
    val text = JLabel(MULTIPLE_CHOICE_LABEL, SwingConstants.LEFT)
    jPanel.add(text)

    for ((index, variant) in this.choiceVariants.withIndex()) {
      val checkBox = createCheckBox(variant, index, this)
      jPanel.add(checkBox)
    }
  }
  else {
    val text = JLabel(SINGLE_CHOICE_LABEL, SwingConstants.LEFT)
    jPanel.add(text)

    val group = ButtonGroup()
    for ((index, variant) in this.choiceVariants.withIndex()) {
      val checkBox = createRadioButton(variant, index, group, this)
      jPanel.add(checkBox)
    }
  }

  return jPanel
}

fun createCheckBox(variant: String?, index: Int, task: ChoiceTask): JCheckBox {
  val checkBox = JCheckBox(variant)
  checkBox.isSelected = task.selectedVariants.contains(index)
  checkBox.addItemListener(createListener(task, index))
  return checkBox
}

fun createRadioButton(variant: String, index: Int, group: ButtonGroup, task: ChoiceTask): JRadioButton {
  val button = JRadioButton(variant)
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
