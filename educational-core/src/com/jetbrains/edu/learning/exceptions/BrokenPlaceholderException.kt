package com.jetbrains.edu.learning.exceptions

import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder

class BrokenPlaceholderException(override val message: String, val placeholder: AnswerPlaceholder) : IllegalStateException() {
  val placeholderInfo
    get() = "Broken placeholder ${placeholder.index + 1} of ${placeholder.taskFile.answerPlaceholders.size}, " +
            "offset ${placeholder.offset}, length ${placeholder.length}."
}