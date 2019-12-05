package com.jetbrains.edu.learning.exceptions

import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder

class BrokenPlaceholderException(override val message: String, val placeholder: AnswerPlaceholder) : IllegalStateException() {
  val placeholderInfo
    get() = """
    Broken placeholder info:
    length: ${placeholder.length}
    offset: ${placeholder.offset}
  """.trimIndent()
}