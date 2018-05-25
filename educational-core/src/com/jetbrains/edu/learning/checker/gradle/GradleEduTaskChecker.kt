package com.jetbrains.edu.learning.checker.gradle

import com.intellij.execution.ExecutionException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckResult.FAILED_TO_CHECK
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

class GradleEduTaskChecker(task: EduTask, project: Project) : TaskChecker<EduTask>(task, project) {
  override fun check(): CheckResult {
    val taskName = "${getGradleProjectName(task)}:test"
    val cmd = generateGradleCommandLine(
      project,
      taskName
    ) ?: return FAILED_TO_CHECK

    return try {
      return parseTestsOutput(cmd.createProcess(), cmd.commandLineString, taskName)
    } catch (e: ExecutionException) {
      Logger.getInstance(GradleEduTaskChecker::class.java).info(CheckUtils.FAILED_TO_CHECK_MESSAGE, e)
      FAILED_TO_CHECK
    }
  }
}
