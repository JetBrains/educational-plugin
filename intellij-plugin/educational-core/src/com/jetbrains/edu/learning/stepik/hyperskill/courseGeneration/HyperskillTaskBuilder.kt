package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.intellij.lang.Language
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFileErrorHighlightLevel
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask.Companion.PYCHARM_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.RemoteEduTask.Companion.REMOTE_EDU_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.stepik.PyCharmStepOptions
import com.jetbrains.edu.learning.stepik.StepikTaskBuilder
import com.jetbrains.edu.learning.stepik.hasHeaderOrFooter
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_COMMENT_ANCHOR
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillLanguages
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStepSource
import com.jetbrains.edu.learning.stepik.hyperskill.getUnsupportedTaskDescriptionText
import com.jetbrains.edu.learning.stepik.hyperskill.stepLink

class HyperskillTaskBuilder(
  course: Course,
  private val stepSource: HyperskillStepSource
) : StepikTaskBuilder(course, stepSource) {
  override fun getLanguageName(language: Language): String? {
    return HyperskillLanguages.getLanguageName(language.id)
  }

  fun build(): Task? {
    val blockName = stepSource.block?.name ?: return null

    val type = if (blockName == PYCHARM_TASK_TYPE && stepSource.isRemoteTested) {
      REMOTE_EDU_TASK_TYPE
    }
    else {
      blockName
    }

    return createTask(type)
  }

  override fun createTask(type: String): Task {
    val task = super.createTask(type)

    task.descriptionText = "<div class=\"step-text\">\n${task.descriptionText}\n</div>"
    task.apply {
      if (stepSource.isCompleted) {
        status = CheckStatus.Solved
      }

      when (this) {
        is CodeTask, is DataTask, is ChoiceTask, is StringTask, is NumberTask, is SortingTask, is MatchingTask, is TableTask -> {
          name = stepSource.title
        }
        is EduTask -> {
          if (task is RemoteEduTask) {
            task.checkProfile = stepSource.checkProfile
          }
          name = stepSource.title
          customPresentableName = null
        }
        is UnsupportedTask -> {
          descriptionText = getUnsupportedTaskDescriptionText(name, stepSource.id)
          name = stepSource.title
        }
      }

      feedbackLink = "${stepLink(stepSource.id)}$HYPERSKILL_COMMENT_ANCHOR"
    }

    if (task is CodeTask) {
      val submissionLanguage = task.submissionLanguage
      if (submissionLanguage != null) {
        val options = stepSource.block?.options as? PyCharmStepOptions
        if (options?.hasHeaderOrFooter(submissionLanguage) == true) {
          doNotHighlightErrorsInTasksWithHeadersOrFooters(task)
        }
      }
    }

    return task
  }

  private fun doNotHighlightErrorsInTasksWithHeadersOrFooters(task: Task) {
    for ((_, taskFile) in task.taskFiles) {
      taskFile.errorHighlightLevel = EduFileErrorHighlightLevel.NONE
    }
  }
}
