package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.ext.getText
import com.jetbrains.edu.learning.courseFormat.tasks.AnswerTask
import com.jetbrains.edu.learning.courseFormat.tasks.NumberTask
import com.jetbrains.edu.learning.messages.EduCoreBundle

fun AnswerTask.getInputAnswer(project: Project): String {
  val answerTaskFile = getTaskFile(AnswerTask.ANSWER_FILE_NAME)
  if (answerTaskFile == null) {
    LOG.warn("Task with answer: ${itemType} for task is null")
    return ""
  }

  val answerPlaceholder = answerTaskFile.answerPlaceholders.firstOrNull()
  if (answerPlaceholder == null) {
    LOG.warn("Answer placeholder in file: ${itemType} for task is null or empty")
    return ""
  }

  val offset = answerPlaceholder.offset
  val endOffset = answerPlaceholder.endOffset

  return answerTaskFile.getText(project)?.substring(offset, endOffset) ?: ""
}

fun AnswerTask.validateAnswer(project: Project): String? {
  val answer = getInputAnswer(project)
  val validationMessage = validateAnswer(answer)

  if (validationMessage != null) {
    return validationMessage
  }
  if (this is NumberTask && answer.trim().replace(',', '.').toDoubleOrNull() == null) {
    return EduCoreBundle.message("hyperskill.number.task.not.number")
  }
  return null
}

private fun validateAnswer(answer: String): String? {
  if (answer.isBlank()) {
    return EduCoreBundle.message("hyperskill.string.task.empty.text")
  }
  return null
}

private val LOG: Logger = logger<HyperskillSubmitConnector>()