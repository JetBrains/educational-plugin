package com.jetbrains.edu.learning.taskToolWindow.ui.specificTaskSwingPanels

import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.tasks.TableTask
import com.jetbrains.edu.learning.taskToolWindow.ui.createButton
import java.awt.event.ItemEvent
import java.awt.event.ItemListener

class TableTaskSpecificPanel(task: TableTask): Wrapper() {
  init {
    val panel = panel {
      createHeader(task)
        .layout(RowLayout.PARENT_GRID)

      for (rowIndex in task.rows.indices) {
        buttonsGroup {
          separator()
          createRow(task, rowIndex)
            .layout(RowLayout.PARENT_GRID)
        }
      }
    }.apply {
      isOpaque = false
      border = JBUI.Borders.empty(4, 0, 10, 10)
    }
    setContent(panel)
  }

  private fun Panel.createHeader(task: TableTask): Row = row {
    cell(Wrapper())
      .align(Align.CENTER)
    for (columnIndex in task.columns.indices) {
      text(task.columns[columnIndex])
        .align(Align.CENTER)
    }
  }

  private fun Panel.createRow(task: TableTask, rowIndex: Int): Row = row {
    text(task.rows[rowIndex])
      .align(AlignX.LEFT + AlignY.CENTER)
    for (columnIndex in task.columns.indices) {
      createButton(task.isMultipleChoice)
        .align(Align.CENTER)
        .component.apply {
          isSelected = task.selected[rowIndex][columnIndex]
          addItemListener(createListener(task, rowIndex, columnIndex))
        }
    }
  }

  private fun createListener(task: TableTask, rowIndex: Int, columnIndex: Int): ItemListener {
    return ItemListener { event ->
      task.selected[rowIndex][columnIndex] = (event.stateChange == ItemEvent.SELECTED)
    }
  }
}