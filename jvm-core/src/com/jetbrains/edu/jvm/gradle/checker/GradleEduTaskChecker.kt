package com.jetbrains.edu.jvm.gradle.checker

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.messages.EduJVMBundle
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckResult.Companion.failedToCheck
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

open class GradleEduTaskChecker(task: EduTask, protected val envChecker: EnvironmentChecker, project: Project) :
  TaskChecker<EduTask>(task, project) {

  override fun check(indicator: ProgressIndicator): CheckResult {
    val possibleError = envChecker.getEnvironmentError(project, task)
    if (possibleError != null) {
      return possibleError
    }

    val (taskName, params) = getGradleTask()

    val testDirs = task.course.configurator?.testDirs ?: return failedToCheck
    val hasTestFiles = task.taskFiles.any { (path, _) -> testDirs.any { path.startsWith(it) } }
    if (!hasTestFiles) {
      return CheckResult(CheckStatus.Solved, EduJVMBundle.message("task.marked.completed"))
    }

    return GradleCommandLine.create(project, taskName, *params.toTypedArray())?.launchAndCheck(indicator) ?: failedToCheck
  }

  protected open fun getGradleTask() = GradleTask(":${getGradleProjectName(task)}:$TEST_TASK_NAME")

  protected data class GradleTask(val taskName: String, val params: List<String> = emptyList())
}
