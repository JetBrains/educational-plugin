package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.intellij.lang.Language
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.PyCharmStepOptions
import com.jetbrains.edu.learning.stepik.StepikTaskBuilder
import com.jetbrains.edu.learning.stepik.hasHeaderOrFooter
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_COMMENT_ANCHOR
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStepSource
import com.jetbrains.edu.learning.stepik.hyperskill.checker.HyperskillLanguages
import com.jetbrains.edu.learning.stepik.hyperskill.stepLink

class HyperskillTaskBuilder(
  private val course: Course,
  lesson: Lesson,
  private val stepSource: HyperskillStepSource,
  private val stepId: Int
) : StepikTaskBuilder(course, lesson, stepSource, stepId, -1) {
  override fun getLanguageName(language: Language): String? {
    return HyperskillLanguages.langOfId(language.id).langName
  }

  private fun Task.description(langId: String, title: String = name): String = buildString {
    appendLine("<h2>$title</h2>")
    appendLine(descriptionText)

    val options = stepSource.block?.options as? PyCharmStepOptions
    if (options?.hasHeaderOrFooter(langId) == true) {
      appendLine("<b>${EduCoreBundle.message("label.caution")}</b><br><br>")
      appendLine(EduCoreBundle.message("hyperskill.hidden.content", EduCoreBundle.message("check.title")))
      appendLine("<br><br>")
    }
  }

  override fun createTask(type: String): Task? {
    val task = super.createTask(type) ?: return null
    task.apply {
      if (stepSource.isCompleted) {
        status = CheckStatus.Solved
      }

      when (this) {
        is CodeTask -> {
          name = stepSource.title
          descriptionText = description(this@HyperskillTaskBuilder.course.languageID)
        }
        is TheoryTask -> {
          descriptionText = description(this@HyperskillTaskBuilder.course.languageID, title = stepSource.title ?: name)
        }
        is EduTask -> {
          name = stepSource.title
        }
      }

      feedbackLink = "${stepLink(stepId)}$HYPERSKILL_COMMENT_ANCHOR"
    }
    return task
  }
}
