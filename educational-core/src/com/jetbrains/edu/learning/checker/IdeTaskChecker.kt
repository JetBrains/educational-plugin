package com.jetbrains.edu.learning.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.IdeTask

class IdeTaskChecker(task: IdeTask, project: Project): TaskChecker<IdeTask>(task, project) {
  override fun check(): CheckResult {
    return CheckResult(CheckStatus.Solved, "Task marked as completed")
  }

  override fun onTaskFailed(message: String) {
  }
}