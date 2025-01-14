package com.jetbrains.edu.aiHints.core.ui

import com.intellij.openapi.project.Project
import com.intellij.util.asSafely
import com.jetbrains.edu.aiHints.core.feedback.dialog.ErrorHintFeedbackDialog
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.jetbrains.annotations.Nls

class ErrorHintInlineBanner(
  project: Project,
  message: @Nls String,
  retryAction: Runnable? = null
) : HintInlineBanner(project, message, Status.Error) {
  init {
    if (retryAction != null) {
      addAction(EduAIHintsCoreBundle.message("hints.label.retry")) {
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
}