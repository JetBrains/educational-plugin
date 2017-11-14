package com.jetbrains.edu.kotlin.studio

import com.intellij.execution.ExecutionException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.kotlin.KtTaskChecker
import com.jetbrains.edu.kotlin.KtTaskChecker.FAILED_TO_LAUNCH
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class KtTaskChecker : StudioTaskCheckerBase() {

  override fun isAccepted(task: Task) = task is EduTask && super.isAccepted(task)

  override fun check(task: Task, project: Project): CheckResult {
    val cmd = generateGradleCommandLine(
            project,
            "${getGradleProjectName(task)}:test"
    ) ?: return FAILED_TO_LAUNCH

    return try {
      val output = CheckUtils.getTestOutput(cmd.createProcess(),
              cmd.commandLineString, false)
      CheckResult(if (output.isSuccess) CheckStatus.Solved else CheckStatus.Failed, output.message)
    } catch (e: ExecutionException) {
      Logger.getInstance(KtTaskChecker::class.java).info(CheckAction.FAILED_CHECK_LAUNCH, e)
      FAILED_TO_LAUNCH
    }
  }
}
