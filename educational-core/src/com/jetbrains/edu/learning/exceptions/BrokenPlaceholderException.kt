package com.jetbrains.edu.learning.exceptions

import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.Nls

class BrokenPlaceholderException(
  @Nls(capitalization = Nls.Capitalization.Sentence) override val message: String,
  val placeholder: AnswerPlaceholder
) : IllegalStateException() {
  val placeholderInfo: String
    @Nls(capitalization = Nls.Capitalization.Sentence)
    get() {
      val placeholdersCount = placeholder.taskFile.answerPlaceholders.size
      return if (placeholdersCount == 1)
        EduCoreBundle.message(
          "exception.message.placeholder.info.single",
          placeholder.offset, placeholder.length
        )
      else
        EduCoreBundle.message(
          "exception.message.placeholder.info.one.of",
          placeholder.index + 1, placeholdersCount, placeholder.offset, placeholder.length
        )
    }
}