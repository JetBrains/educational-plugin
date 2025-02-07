package com.jetbrains.edu.ai.learner.feedback.listener

import com.intellij.openapi.project.Project
import com.jetbrains.edu.ai.learner.feedback.AILearnerFeedbackService
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class AILearnerFeedbackCheckListener : CheckListener {
  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    AILearnerFeedbackService.getInstance(project).showFeedbackInClippy(positive = result.isSolved)
  }
}