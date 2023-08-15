package com.jetbrains.edu.learning.courseFormat.tasks

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import org.jetbrains.annotations.NonNls
import java.util.*

class TableTask : Task {
  override val itemType: String = TABLE_TASK_TYPE

  var isCheckbox = false

  var rows = emptyList<String>()

  var columns = emptyList<String>()

  var isSelected = arrayOf<Array<Boolean>>()

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
      LOG.error("There was an attempt to choose an cell with invalid indices")
      return
    }
    if (!isCheckbox) {
      clearSelectedInRow(rowIndex)
    }
    isSelected[rowIndex][columnIndex] = true
  }

  fun clearSelectedVariants() {
    for (rowIndex in isSelected.indices) {
      clearSelectedInRow(rowIndex)
    }
  }

  private fun clearSelectedInRow(rowIndex: Int) {
    isSelected[rowIndex].fill(false)
  }

  fun createTable(rows: List<String>, columns: List<String>, isCheckbox: Boolean = false) {
    this.rows = rows
    this.columns = columns
    this.isCheckbox = isCheckbox
    isSelected = Array(rows.size) {
      Array(columns.size) { false }
    }
  }

  companion object {
    @NonNls
    const val TABLE_TASK_TYPE: String = "table"

    private val LOG: Logger = Logger.getInstance(TableTask::class.java)
  }
}