package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.intellij.lang.Language
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepikTaskBuilder
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStepSource
import com.jetbrains.edu.learning.stepik.hyperskill.checker.HyperskillLanguages
import com.jetbrains.edu.learning.stepik.hyperskill.stepLink
import com.jetbrains.edu.learning.taskDescription.link

class HyperskillTaskBuilder(course: Course, lesson: Lesson, private val stepSource: HyperskillStepSource, private val stepId: Int)
  : StepikTaskBuilder(course, lesson, stepSource, stepId, -1) {
  override fun getLanguageName(language: Language): String? {
    return HyperskillLanguages.langOfId(language.id).langName
  }

  private fun Task.description(theoryId: Int?): String = buildString {
    appendln("<b>$name</b> ${link(stepLink(id), "Open on ${EduNames.JBA}", true)}")
    appendln("<br><br>")
    appendln(descriptionText)
    if (theoryId != null) {
      append(link(stepLink(theoryId), "Show topic summary"))
    }
  }

  override fun createTask(type: String): Task? {
    val task = super.createTask(type)
    if (task is CodeTask) {
      task.apply {
        name = stepSource.title
        descriptionText = description(stepSource.topicTheory)
        feedbackLink = FeedbackLink(stepLink(stepId))
      }
    }
    return task
  }
}
