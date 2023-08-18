package com.jetbrains.edu.learning.courseFormat.tasks

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import java.util.*

/**
 * Tasks with a number input
 */
class NumberTask : AnswerTask {

  constructor() : super()

  constructor(name: String) : super(name)

  constructor(name: String,
              id: Int,
              position: Int,
              updateDate: Date,
              status: CheckStatus = CheckStatus.Unchecked
  ) : super(name, id, position, updateDate, status)

  override val itemType: String = NUMBER_TASK_TYPE

  companion object {
    const val NUMBER_TASK_TYPE: String = "number"
  }
}