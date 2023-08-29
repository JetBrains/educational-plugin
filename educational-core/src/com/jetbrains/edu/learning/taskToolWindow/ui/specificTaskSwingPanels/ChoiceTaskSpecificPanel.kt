package com.jetbrains.edu.learning.taskToolWindow.ui.specificTaskSwingPanels

import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.Gaps
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.taskToolWindow.ui.addBorder
import com.jetbrains.edu.learning.taskToolWindow.ui.createButton
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.*

class ChoiceTaskSpecificPanel(task: ChoiceTask) : Wrapper() {
  init {
    val panel = panel {
      buttonsGroup {
        row {
          text(task.presentableQuizHeader).apply { isOpaque = false }
        }
        for (index in task.choiceOptions.indices) {
          optionRow(task, index)
            .layout(RowLayout.PARENT_GRID)
        }
      }
    }.apply {
      addBorder(JBUI.Borders.empty(15, 0, 10, 10))
      isOpaque = false
    }
    setContent(panel)
  }

  private fun Panel.optionRow(
    task: ChoiceTask,
    index: Int
  ): Row = row {
    val choiceOption = task.choiceOptions[index]
    val areOptionsEnabled = task.status != CheckStatus.Failed
    val isMultipleChoice = task.isMultipleChoice
    createButton(isMultipleChoice)
      .widthGroup("buttonsGroup")
      .enabled(areOptionsEnabled)
      .customize(Gaps(right = 8))
      .component
      .apply {
        isSelected = task.selectedVariants.contains(index)
        addItemListener(createListener(task, index))
        isOpaque = false
      }
    text(choiceOption.text)
      .resizableColumn()
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