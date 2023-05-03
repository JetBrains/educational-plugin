package com.jetbrains.edu.learning.taskDescription.ui.specificTaskSwingPanels

import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.*

class ChoiceTaskSpecificPanel(task: ChoiceTask) : Wrapper() {
  init {
    val panel = NonOpaquePanel(VerticalFlowLayout())
      .apply { border = JBUI.Borders.empty(15, 0, 10, 10) }
      .addBox(task, task.isMultipleChoice)
    setContent(panel)
  }

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
    createTopicForSpecificPanel(task.presentableQuizHeader)
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
}