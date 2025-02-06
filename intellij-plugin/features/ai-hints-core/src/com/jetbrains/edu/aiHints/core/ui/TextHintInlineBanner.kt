package com.jetbrains.edu.aiHints.core.ui

import com.intellij.openapi.project.Project
import com.intellij.util.asSafely
import com.jetbrains.edu.ai.translation.ui.LikeBlock
import com.jetbrains.edu.aiHints.core.feedback.dialog.TextHintFeedbackDialog
import com.jetbrains.edu.aiHints.core.log.Logger
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
        val score = dialog.getLikenessAnswer() ?: likeness
        Logger.aiHintsLogger.info(
          """|| Course id: ${task.course.id} | Lesson id: ${task.lesson.id} | Task id: ${task.id}
           || Hint Score: ${score.result}
           || Text hint: ${textHint.text}
        """.trimMargin()
        )
        score
      } else {
        LikeBlock.FeedbackLikenessAnswer.NO_ANSWER
      }
    }
    return this
  }
}