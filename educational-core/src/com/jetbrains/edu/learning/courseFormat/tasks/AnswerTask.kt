package com.jetbrains.edu.learning.courseFormat.tasks

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import java.util.*

sealed class AnswerTask : Task {

  constructor() : super()

  constructor(name: String) : super(name)

  constructor(name: String,
              id: Int,
              position: Int,
              updateDate: Date,
              status: CheckStatus = CheckStatus.Unchecked
  ) : super(name, id, position, updateDate, status)

  companion object {
    const val ANSWER_FILE_NAME: String = "answer.txt"
  }
}
