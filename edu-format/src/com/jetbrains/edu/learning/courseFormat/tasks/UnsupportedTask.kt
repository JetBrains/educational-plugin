package com.jetbrains.edu.learning.courseFormat.tasks

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import java.util.*

class UnsupportedTask : Task {
  constructor()

  constructor(name: String) : super(name)

  constructor(name: String, id: Int, position: Int, updateDate: Date, status: CheckStatus) :
    super(name, id, position, updateDate, status)

  override val itemType: String = UNSUPPORTED_TASK_TYPE

  companion object {
    const val UNSUPPORTED_TASK_TYPE: String = "unsupported"
  }
}