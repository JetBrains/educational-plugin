package com.jetbrains.edu.aiHints.core.ui

import com.intellij.openapi.project.Project
import com.intellij.util.asSafely
import com.jetbrains.edu.aiHints.core.feedback.dialog.TextHintFeedbackDialog
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.educational.ml.hints.hint.TextHint
import org.jetbrains.annotations.Nls

class TextHintInlineBanner(
  project: Project,
  task: Task,
  message: @Nls String,
) : HintInlineBanner(project, task, message) {
  fun addFeedbackLink(task: Task, studentSolution: String, textHint: TextHint): TextHintInlineBanner {
    val project = task.project ?: return this
    val course = project.course.asSafely<EduCourse>() ?: return this
    addAction(EduAIHintsCoreBundle.message("hints.feedback.action.link")) {
      TextHintFeedbackDialog(project, course, task, studentSolution, textHint).show()
    }
    return this
  }
}
