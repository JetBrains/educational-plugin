package com.jetbrains.edu.learning.courseFormat.tasks.matching

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.logger
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.jetbrains.annotations.VisibleForTesting
import java.util.*

sealed class SortingBasedTask : Task {
  /**
   * Texts of options
   */
  var options = emptyList<String>()
    set(value) {
      ordering = IntArray(value.size) { it }
      field = value
    }

  /**
   * Indexes of options in the order of the selected sorting in the column
   *
   * ordering.get(columnIndex) = optionIndex
   */
  var ordering = intArrayOf()

  constructor() : super()

  constructor(name: String) : super(name)

  constructor(
    name: String,
    id: Int,
    position: Int,
    updateDate: Date,
    status: CheckStatus = CheckStatus.Unchecked
  ) : super(name, id, position, updateDate, status)

  /**
   * @return List of options in the order of the selected sorting
   */
  fun getOrderedOptions(): List<String> = List(options.size) { options[ordering[it]] }

  /**
   * Move option from right side at position [index] one position up
   */
  fun moveOptionUp(index: Int) {
    val targetIndex = index - 1
    if (targetIndex < 0) {
      val message = "There was an attempt to move the option up out of bounds"
      LOG.severe(message)
      throw Throwable(message)
    }
    swapOptions(index, targetIndex)
  }

  /**
   * Move option from right side at position [index] one position down
   */
  fun moveOptionDown(index: Int) {
    val targetIndex = index + 1
    if (targetIndex == options.size) {
      val message = "There was an attempt to move the option down out of bounds"
      LOG.severe(message)
      throw Throwable(message)
    }
    swapOptions(index, targetIndex)
  }

  /**
   * Swap options at column indexes [firstColumnIndex] and [secondColumnIndex]
   */
  @VisibleForTesting
  fun swapOptions(firstColumnIndex: Int, secondColumnIndex: Int) {
    val firstOptionIndex = ordering[firstColumnIndex]
    val secondOptionIndex = ordering[secondColumnIndex]

    ordering[firstColumnIndex] = secondOptionIndex
    ordering[secondColumnIndex] = firstOptionIndex
  }

  fun restoreInitialOrdering() {
    ordering = IntArray(options.size) { it }
  }

  companion object {
    private val LOG = logger<SortingBasedTask>()
  }
}