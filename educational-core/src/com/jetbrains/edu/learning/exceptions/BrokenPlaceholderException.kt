package com.jetbrains.edu.learning.exceptions

import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder

class BrokenPlaceholderException(override val message: String, val placeholder: AnswerPlaceholder) : IllegalStateException() {
  val placeholderInfo
    get() = "Placeholder ${placeholder.index + 1} of ${placeholder.taskFile.answerPlaceholders.size} is broken " +
            "(offset ${placeholder.offset}, length ${placeholder.length})."
}