package com.jetbrains.edu.learning.exceptions

import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import java.lang.IllegalStateException

class BrokenPlaceholderException(override val message: String, val placeholder: AnswerPlaceholder) : IllegalStateException()