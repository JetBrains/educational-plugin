package com.jetbrains.edu.aiHints.core.ui

import com.intellij.openapi.project.Project
import com.intellij.util.asSafely
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.edu.ai.translation.statistics.EduAIFeaturesEventFields.HintBannerType
import com.jetbrains.edu.ai.translation.ui.LikeBlock
import com.jetbrains.edu.ai.ui.HintInlineBanner
import com.jetbrains.edu.aiHints.core.feedback.dialog.TextHintFeedbackDialog
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
  fun addFeedbackLikenessButtons(task: Task, studentSolution: String, textHint: TextHint): TextHintInlineBanner {
    val project = task.project ?: return this
    val course = project.course.asSafely<EduCourse>() ?: return this
    addLikeDislikeActions {
      val dialog = TextHintFeedbackDialog(project, course, task, studentSolution, textHint, likeness)
      if (dialog.showAndGet()) {
        dialog.getLikenessAnswer() ?: likeness
      } else {
        LikeBlock.FeedbackLikenessAnswer.NO_ANSWER
      }
    }
    return this
  }

  @RequiresEdt
  fun display() {
    super.display(HintBannerType.TEXT)
  }
}