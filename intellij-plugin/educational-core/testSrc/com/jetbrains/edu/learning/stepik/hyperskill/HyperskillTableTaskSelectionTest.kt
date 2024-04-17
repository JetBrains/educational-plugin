package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.tasks.TableTask
import org.junit.Test

class HyperskillTableTaskSelectionTest: EduTestCase() {
  @Test
  fun `test initial selection`() {
    val task = createTableTask(false)
    assertEquals(task.getSelectedCells(), listOf<Pair<Int, Int>>())
  }

  @Test
  fun `test select in checkbox task`() {
    val task = createTableTask(true)
    task.apply {
      choose(0, 1)
      choose(0, 2)
      choose(0, 2)
      choose(1, 0)
    }
    assertEquals(task.getSelectedCells(), listOf(Pair(0, 1), Pair(1, 0)))
  }

  @Test
  fun `test select in non-checkbox task`() {
    val task = createTableTask(false)
    task.apply {
      choose(0, 1)
      choose(0, 2)
      choose(0, 2)
      choose(1, 0)
    }
    assertEquals(task.getSelectedCells(), listOf(Pair(0, 2), Pair(1, 0)))
  }

  private fun TableTask.getSelectedCells(): List<Pair<Int, Int>> {
    return selected.flatMapIndexed { rowIndex, column ->
      column.mapIndexed { columnIndex, value ->
        if (value) {
          Pair(rowIndex, columnIndex)
        }
        else null
      }.filterNotNull()
    }
  }

  private fun createTableTask(isCheckbox: Boolean = false) = TableTask().apply {
    createTable(
      rows = listOf("A", "B"),
      columns = listOf("1", "2", "3"),
      isCheckbox = isCheckbox
    )
  }
}