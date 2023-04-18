package com.jetbrains.edu.learning.courseFormat.tasks.matching

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import org.jetbrains.annotations.NonNls
import java.util.*

class SortingTask : SortingBasedTask {
  constructor() : super()

  constructor(name: String) : super(name)

  constructor(
    name: String,
    id: Int,
    position: Int,
    updateDate: Date,
    status: CheckStatus = CheckStatus.Unchecked
  ) : super(name, id, position, updateDate, status)

  override val itemType: String = SORTING_TASK_TYPE

  companion object {
    @NonNls
    const val SORTING_TASK_TYPE: String = "sorting"
  }
}