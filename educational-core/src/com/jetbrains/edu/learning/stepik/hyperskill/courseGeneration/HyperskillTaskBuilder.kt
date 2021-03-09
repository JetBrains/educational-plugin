package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.intellij.lang.Language
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.PyCharmStepOptions
import com.jetbrains.edu.learning.stepik.StepikTaskBuilder
import com.jetbrains.edu.learning.stepik.hasHeaderOrFooter
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_COMMENT_ANCHOR
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStepSource
import com.jetbrains.edu.learning.stepik.hyperskill.checker.HyperskillLanguages
import com.jetbrains.edu.learning.stepik.hyperskill.stepLink
import com.jetbrains.edu.learning.taskDescription.link

class HyperskillTaskBuilder(
  private val course: Course,
  lesson: Lesson,
  private val stepSource: HyperskillStepSource,
  private val stepId: Int
) : StepikTaskBuilder(course, lesson, stepSource, stepId, -1) {
  override fun getLanguageName(language: Language, languageVersion: String): String? {
    return HyperskillLanguages.langOfId(language.id).langName
  }

  private fun Task.description(theoryId: Int?, langId: String): String = buildString {
    appendln("<h2>$name</h2>")
    appendln(descriptionText)

    val options = stepSource.block?.options as? PyCharmStepOptions
    if (options?.hasHeaderOrFooter(langId) == true) {
      appendln("<b>${EduCoreBundle.message("label.caution")}</b><br><br>")
      appendln(EduCoreBundle.message("hyperskill.hidden.content", EduCoreBundle.message("check.title")))
      appendln("<br><br>")
    }

    if (theoryId != null) {
      append(link(stepLink(theoryId), "Show topic summary"))
    }
  }

  override fun createTask(type: String): Task? {
    val task = super.createTask(type)
    task?.apply {
      if (stepSource.isCompleted) {
        status = CheckStatus.Solved
      }
    }

    if (task is CodeTask) {
      task.apply {
        name = stepSource.title
        descriptionText = description(stepSource.topicTheory, this@HyperskillTaskBuilder.course.languageID)
        feedbackLink = FeedbackLink("${stepLink(stepId)}$HYPERSKILL_COMMENT_ANCHOR")
      }
    }
    return task
  }
}
