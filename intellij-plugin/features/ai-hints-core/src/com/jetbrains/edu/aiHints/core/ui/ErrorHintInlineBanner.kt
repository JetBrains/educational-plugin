package com.jetbrains.edu.aiHints.core.ui

import com.intellij.openapi.project.Project
import com.intellij.util.asSafely
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.edu.ai.translation.statistics.EduAIFeaturesCounterUsageCollector
import com.jetbrains.edu.ai.translation.statistics.EduAIFeaturesEventFields.HintBannerType
import com.jetbrains.edu.ai.ui.HintInlineBanner
import com.jetbrains.edu.aiHints.core.feedback.dialog.ErrorHintFeedbackDialog
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.jetbrains.annotations.Nls

class ErrorHintInlineBanner(
  project: Project,
  task: Task,
  message: @Nls String,
  retryAction: Runnable? = null
) : HintInlineBanner(project, task, message, Status.Error) {
  init {
    if (retryAction != null) {
      addAction(EduAIHintsCoreBundle.message("hints.label.retry")) {
        EduAIFeaturesCounterUsageCollector.hintRetryClicked(task)
        close()
        retryAction.run()
      }
    }
  }

  fun addFeedbackLink(task: Task, taskFileText: String, errorMessage: String): ErrorHintInlineBanner {
    val project = task.project ?: return this
    val course = project.course.asSafely<EduCourse>() ?: return this
    addAction(EduAIHintsCoreBundle.message("hints.feedback.action.link")) {
      ErrorHintFeedbackDialog(project, course, task, taskFileText, errorMessage).show()
    }
    return this
  }

  @RequiresEdt
  fun display() {
    super.display(HintBannerType.ERROR)
  }
}