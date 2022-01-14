package com.jetbrains.edu.learning.courseFormat.tasks

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.messages.EduCoreBundle
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

  override fun getItemType(): String = NUMBER_TASK_TYPE

  override fun validateAnswer(project: Project): String? {
    val answer = getInputAnswer(project)
    val validationMessage = super.validateAnswer(answer)

    if (validationMessage != null) {
      return validationMessage
    }

    if (answer.trim().replace(',', '.').toDoubleOrNull() == null) {
      return EduCoreBundle.message("hyperskill.number.task.not.number")
    }
    return null
  }

  companion object {
    const val NUMBER_TASK_TYPE: String = "number"
  }
}