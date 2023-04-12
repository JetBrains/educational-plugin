package com.jetbrains.edu.coursecreator

import com.jetbrains.edu.learning.placeholder.PlaceholderPainter.getPaintedPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile

object CCTestsUtil {
  private fun getPlaceholderPresentation(placeholder: AnswerPlaceholder): String {
    return "offset=${placeholder.offset} " +
           "length=${placeholder.length} " +
           "possibleAnswer=${placeholder.possibleAnswer} " +
           "placeholderText=${placeholder.placeholderText}"
  }

  @JvmStatic
  fun checkPainters(placeholder: AnswerPlaceholder) {
    val paintedPlaceholders = getPaintedPlaceholder()
    if (paintedPlaceholders.contains(placeholder)) return
    for (paintedPlaceholder in paintedPlaceholders) {
      if (paintedPlaceholder.offset == placeholder.offset &&
          paintedPlaceholder.length == placeholder.length) {
        return
      }
    }
    throw AssertionError("No highlighter for placeholder: " + getPlaceholderPresentation(placeholder))
  }

  @JvmStatic
  fun checkPainters(taskFile: TaskFile) {
    val paintedPlaceholders = getPaintedPlaceholder()
    for (answerPlaceholder in taskFile.answerPlaceholders) {
      if (!paintedPlaceholders.contains(answerPlaceholder)) {
        throw AssertionError("No highlighter for placeholder: " + getPlaceholderPresentation(answerPlaceholder))
      }
    }
  }
}