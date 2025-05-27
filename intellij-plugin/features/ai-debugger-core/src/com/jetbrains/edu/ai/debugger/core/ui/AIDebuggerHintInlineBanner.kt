package com.jetbrains.edu.ai.debugger.core.ui

import com.intellij.openapi.project.Project
import com.jetbrains.edu.ai.debugger.core.feedback.AIDebuggerFeedbackDialog
import com.jetbrains.edu.ai.debugger.core.service.TestInfo
import com.jetbrains.edu.ai.translation.ui.LikeBlock
import com.jetbrains.edu.ai.ui.HintInlineBanner
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.educational.ml.debugger.dto.Breakpoint
import com.jetbrains.educational.ml.debugger.response.BreakpointHintDetails
import org.jetbrains.annotations.Nls

class AIDebuggerHintInlineBanner(
  project: Project,
  task: Task,
  message: @Nls String,
) : HintInlineBanner(project, task, message) {

  fun addFeedbackLikenessButtons(
    task: Task,
    userSolution: Map<String, String>,
    testInfo: TestInfo,
    finalBreakpoints: List<Breakpoint>,
    intermediateBreakpoints: Map<String, List<Int>>,
    breakpointHints: List<BreakpointHintDetails>
  ): AIDebuggerHintInlineBanner {
    val project = task.project ?: return this
    addLikeDislikeActions {
      val dialog = AIDebuggerFeedbackDialog(project, task, userSolution, testInfo,
        finalBreakpoints, intermediateBreakpoints, breakpointHints, likeness)
      if (dialog.showAndGet()) {
        dialog.getLikenessAnswer() ?: likeness
      } else {
        LikeBlock.FeedbackLikenessAnswer.NO_ANSWER
      }.also { close() }
    }
    return this
  }
}
