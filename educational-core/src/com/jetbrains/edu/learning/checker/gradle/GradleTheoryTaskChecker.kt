package com.jetbrains.edu.learning.checker.gradle

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.checker.*
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

class GradleTheoryTaskChecker(
  task: TheoryTask,
  project: Project,
  private val mainClassForFile: (Project, VirtualFile) -> String?
) : TheoryTaskChecker(task, project) {

  override fun onTaskSolved(message: String) {
    // do not show popup
  }

  override fun check(): CheckResult {
    val result = runGradleRunTask(project, task, mainClassForFile)
    val output = when (result) {
      is Err -> return result.error
      is Ok -> result.value
    }

    CheckUtils.showOutputToolWindow(project, output)
    return CheckResult(CheckStatus.Solved, "")
  }
}
