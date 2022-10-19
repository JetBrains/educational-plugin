/*
 * Copyright 2000-2021 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformerContext
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.*

class SwingToolWindow(project: Project) : TaskDescriptionToolWindow(project) {
  override val taskSpecificPanel: JComponent = JPanel(BorderLayout())

  init {
    taskInfoPanel.border = JBUI.Borders.empty(20, 0, 0, 10)
  }

  override fun updateTaskSpecificPanel(task: Task?) {
    taskSpecificPanel.removeAll()
    val panel = createSpecificPanel(task)
    if (panel != null) {
      taskSpecificPanel.add(panel, BorderLayout.CENTER)
      taskSpecificPanel.revalidate()
      taskSpecificPanel.repaint()
    }
  }

  private fun createSpecificPanel(task: Task?): JPanel? {
    val choiceTask = task as? ChoiceTask ?: return null
    return createSpecificPanel(choiceTask)
  }

  private fun createSpecificPanel(choiceTask: ChoiceTask): JPanel =
    NonOpaquePanel(VerticalFlowLayout())
      .apply {
        border = JBUI.Borders.empty(TOP_INSET, LEFT_INSET, BOTTOM_INSET, RIGHT_INSET)
      }
      .addBox(choiceTask, choiceTask.isMultipleChoice)


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
    createTopicForSpecificPanel(task)
    val isEnabled = task.status != CheckStatus.Failed
    for ((index, option) in task.choiceOptions.withIndex()) {
      val box = createBox(option.text).createButton(index, task, enabled = isEnabled, opaque = false, group)
      add(box)
    }
  }

  private fun NonOpaquePanel.createTopicForSpecificPanel(task: ChoiceTask) {
    taskSpecificPanelViewer.setHtmlWithContext(HtmlTransformerContext(project, task))
    taskSpecificPanelViewer.component.isOpaque = false
    add(taskSpecificPanelViewer.component)
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

  companion object {
    private const val LEFT_INSET = 0
    private const val RIGHT_INSET = 10
    private const val TOP_INSET = 15
    private const val BOTTOM_INSET = 10
  }
}