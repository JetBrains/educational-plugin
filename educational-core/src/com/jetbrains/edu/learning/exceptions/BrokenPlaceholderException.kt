package com.jetbrains.edu.learning.exceptions

import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder

class BrokenPlaceholderException(override val message: String, val placeholder: AnswerPlaceholder) : IllegalStateException() {
  val placeholderInfo: String
    get() {
      val placeholdersCount = placeholder.taskFile.answerPlaceholders.size
      val placeholderPosition = if (placeholdersCount == 1) "" else " ${placeholder.index + 1} of ${placeholdersCount}"
      return "Broken placeholder$placeholderPosition, offset ${placeholder.offset}, length ${placeholder.length}."
    }
}