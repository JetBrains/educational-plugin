package com.jetbrains.edu.ai.error.explanation.listener

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task

/**
 * Collects stderr for error explanation after a failed run configuration launched by edu plugin.
 * If it was a test run configuration, then stderr will be collected from test output.
 */
class ErrorExplanationCheckListener : CheckListener {
  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    super.afterCheck(project, task, result)
    if (result.status != CheckStatus.Failed) return
    //set stderr
  }
}