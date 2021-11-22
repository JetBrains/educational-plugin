package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.intellij.lang.Language
import com.jetbrains.edu.learning.EduExperimentalFeatures.HYPERSKILL_CHOICE_TASK_SUPPORT
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask.Companion.PYCHARM_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.PyCharmStepOptions
import com.jetbrains.edu.learning.stepik.StepikTaskBuilder
import com.jetbrains.edu.learning.stepik.hasHeaderOrFooter
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_COMMENT_ANCHOR
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillLanguages
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStepSource
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask.Companion.REMOTE_EDU_TASK_TYPE
import com.jetbrains.edu.learning.stepik.hyperskill.stepLink

class HyperskillTaskBuilder(
  private val course: Course,
  lesson: Lesson,
  private val stepSource: HyperskillStepSource
) : StepikTaskBuilder(course, lesson, stepSource, stepSource.id, -1) {
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

  fun build(): Task? {
    val blockName = stepSource.block?.name ?: return null
    if (blockName == ChoiceTask.CHOICE_TASK_TYPE && !isFeatureEnabled(HYPERSKILL_CHOICE_TASK_SUPPORT)) {
      return null
    }

    val type = if (blockName == PYCHARM_TASK_TYPE && stepSource.isRemoteTested) {
      REMOTE_EDU_TASK_TYPE
    }
    else {
      blockName
    }

    return if (isSupported(type)) createTask(type) else null
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
        is DataTask -> {
          name = stepSource.title
        }
        is EduTask -> {
          if (task is RemoteEduTask) {
            task.checkProfile = stepSource.checkProfile
          }
          name = stepSource.title
        }
        is TheoryTask -> {
          descriptionText = description(this@HyperskillTaskBuilder.course.languageID, title = stepSource.title ?: name)
        }
        is ChoiceTask -> {
          descriptionText = description(this@HyperskillTaskBuilder.course.languageID, stepSource.title ?: name)
          name = stepSource.title
        }
      }

      feedbackLink = "${stepLink(stepSource.id)}$HYPERSKILL_COMMENT_ANCHOR"
    }
    return task
  }
}
