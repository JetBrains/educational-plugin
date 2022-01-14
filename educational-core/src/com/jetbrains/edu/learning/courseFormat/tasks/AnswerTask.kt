package com.jetbrains.edu.learning.courseFormat.tasks

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.getText
import com.jetbrains.edu.learning.messages.EduCoreBundle
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

  fun getInputAnswer(project: Project): String {
    val answerTaskFile = getTaskFile(ANSWER_FILE_NAME)
    if (answerTaskFile == null) {
      LOG.warn("Task with answer: ${itemType} for task is null")
      return ""
    }

    val answerPlaceholder = answerTaskFile.answerPlaceholders?.first()
    if (answerPlaceholder == null) {
      LOG.warn("Answer placeholder in file: ${itemType} for task is null or empty")
      return ""
    }

    val offset = answerPlaceholder.offset
    val endOffset = answerPlaceholder.endOffset

    return answerTaskFile.getText(project)?.substring(offset, endOffset) ?: ""
  }

  open fun validateAnswer(project: Project): String? {
    val answer = getInputAnswer(project)
    return validateAnswer(answer)
  }

  protected fun validateAnswer(answer: String): String? {
    if (answer.isBlank()) {
      return EduCoreBundle.message("hyperskill.string.task.empty.text")
    }
    return null
  }

  companion object {
    const val ANSWER_FILE_NAME: String = "answer.txt"
  }
}
