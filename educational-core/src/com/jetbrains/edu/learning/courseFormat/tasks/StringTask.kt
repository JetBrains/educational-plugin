package com.jetbrains.edu.learning.courseFormat.tasks

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import java.util.*

/**
 * Tasks with an answer input
 */
class StringTask : AnswerTask {

  constructor() : super()

  constructor(name: String) : super(name)

  constructor(name: String,
              id: Int,
              position: Int,
              updateDate: Date,
              status: CheckStatus = CheckStatus.Unchecked
  ) : super(name, id, position, updateDate, status)

  override fun getItemType(): String = STRING_TASK_TYPE

  companion object {
    const val STRING_TASK_TYPE: String = "string"
  }
}