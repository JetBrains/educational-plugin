package com.jetbrains.edu.learning.courseFormat.tasks

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.getText
import java.util.*

/**
 * Tasks with an answer input
 */
class StringTask : Task {

  constructor()

  constructor(name: String) : super(name)

  constructor(name: String, id: Int, position: Int, updateDate: Date, status: CheckStatus) : super(name, id, position, updateDate, status)

  override fun getItemType(): String = STRING_TASK_TYPE

  fun getInputAnswer(project: Project): String {
    val answerTaskFile = getTaskFile(ANSWER_FILE_NAME)
    if (answerTaskFile == null) {
      LOG.warn("Task with answer: $STRING_TASK_TYPE for string task is null")
      return ""
    }

    val answerPlaceholder = answerTaskFile.answerPlaceholders?.first()
    if (answerPlaceholder == null) {
      LOG.warn("Answer placeholder in file: $STRING_TASK_TYPE for string task is null or empty")
      return ""
    }

    val offset = answerPlaceholder.offset
    val endOffset = answerPlaceholder.endOffset

    return answerTaskFile.getText(project)?.substring(offset, endOffset) ?: ""
  }

  companion object {
    const val STRING_TASK_TYPE: String = "string"
    const val ANSWER_FILE_NAME: String = "answer.txt"

    private val LOG = Logger.getInstance(StringTask::class.java)
  }
}