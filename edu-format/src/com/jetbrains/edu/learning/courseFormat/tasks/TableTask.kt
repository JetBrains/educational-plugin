package com.jetbrains.edu.learning.courseFormat.tasks

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.logger
import org.jetbrains.annotations.NonNls
import java.util.Date
import java.util.logging.Logger

class TableTask : Task {
  override val itemType: String = TABLE_TASK_TYPE

  var isMultipleChoice = false

  var rows = emptyList<String>()

  var columns = emptyList<String>()

  var selected = arrayOf<BooleanArray>()

  constructor() : super()

  constructor(name: String) : super(name)

  constructor(
    name: String,
    id: Int,
    position: Int,
    updateDate: Date,
    status: CheckStatus = CheckStatus.Unchecked
  ) : super(name, id, position, updateDate, status)

  fun choose(rowIndex: Int, columnIndex: Int) {
    if (rowIndex !in rows.indices || columnIndex !in columns.indices) {
      LOG.severe("There was an attempt to choose a cell with invalid indices")
      return
    }
    if (!isMultipleChoice) {
      clearSelectedInRow(rowIndex)
    }
    selected[rowIndex][columnIndex] = !selected[rowIndex][columnIndex]
  }

  fun clearSelectedVariants() {
    for (rowIndex in selected.indices) {
      clearSelectedInRow(rowIndex)
    }
  }

  private fun clearSelectedInRow(rowIndex: Int) {
    selected[rowIndex].fill(false)
  }

  fun createTable(rows: List<String>, columns: List<String>, isCheckbox: Boolean = false) {
    this.rows = rows
    this.columns = columns
    this.isMultipleChoice = isCheckbox
    selected = Array(rows.size) {
      BooleanArray(columns.size) { false }
    }
  }

  companion object {
    @NonNls
    const val TABLE_TASK_TYPE: String = "table"

    private val LOG: Logger = logger<TableTask>()
  }
}