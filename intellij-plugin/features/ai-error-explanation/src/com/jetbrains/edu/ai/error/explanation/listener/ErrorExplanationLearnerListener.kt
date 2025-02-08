package com.jetbrains.edu.ai.error.explanation.listener

import com.intellij.openapi.project.Project
import com.jetbrains.edu.ai.error.explanation.ErrorExplanationManager
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class ErrorExplanationLearnerListener : CheckListener {
  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    if (!result.isSolved) {
      ErrorExplanationManager.getInstance(project).showErrorExplanationPanelInClippy(result.fullMessage)
    }
  }
}