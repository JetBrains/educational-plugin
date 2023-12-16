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
      val taskFile = placeholder.taskFile
      val task = taskFile.task
      val lesson = task.lesson
      val section = lesson.section
      val path = listOfNotNull(section?.name, lesson.name, task.name, taskFile.name).joinToString(separator = "/")

      return EduCoreBundle.message("exception.broken.placeholder.message", path, placeholder.offset, placeholder.length)
    }
}