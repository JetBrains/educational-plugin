package com.jetbrains.edu.learning.courseFormat.tasks

import com.jetbrains.edu.learning.checker.OutputTaskChecker
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import java.util.*

/**
 * Task type that allows to test output without any test files.
 * Correct output is specified in output.txt file which is invisible for student.
 * @see OutputTaskChecker
 */
class OutputTask : Task {

  constructor() : super()

  constructor(name: String) : super(name)

  constructor(name: String, id: Int, position: Int, updateDate: Date, status: CheckStatus) : super(name, id, position, updateDate, status)

  companion object {
    const val OUTPUT_TASK_TYPE = "output"
  }

  override fun getItemType() = OUTPUT_TASK_TYPE

  override fun isToSubmitToStepik(): Boolean {
    return myStatus != CheckStatus.Unchecked
  }

  override fun supportSubmissions(): Boolean = true
}
