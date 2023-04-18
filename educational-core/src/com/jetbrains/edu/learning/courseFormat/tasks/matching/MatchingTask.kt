package com.jetbrains.edu.learning.courseFormat.tasks.matching

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import org.jetbrains.annotations.NonNls
import java.util.*

class MatchingTask : SortingBasedTask {
  var captions = emptyList<String>()

  constructor() : super()

  constructor(name: String) : super(name)

  constructor(
    name: String,
    id: Int,
    position: Int,
    updateDate: Date,
    status: CheckStatus = CheckStatus.Unchecked
  ) : super(name, id, position, updateDate, status)

  override val itemType: String = MATCHING_TASK_TYPE

  companion object {
    @NonNls
    const val MATCHING_TASK_TYPE: String = "matching"
  }
}