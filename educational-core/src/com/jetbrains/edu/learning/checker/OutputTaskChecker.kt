package com.jetbrains.edu.learning.checker

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.checker.CheckResult.Companion.FAILED_TO_CHECK
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.handlers.CodeExecutor


open class OutputTaskChecker(
  task: OutputTask,
  project: Project,
  protected val codeExecutor: CodeExecutor
) : TaskChecker<OutputTask>(task, project) {

  override fun check(indicator: ProgressIndicator): CheckResult {
    try {
      val outputString = when (val result = codeExecutor.execute(project, task)) {
        is Ok -> result.value
        is Err -> return CheckResult(CheckStatus.Unchecked, result.error)
      }

      val outputPatternFile = task.getTaskDir(project)?.findChild(OUTPUT_PATTERN_NAME)
                              ?: return FAILED_TO_CHECK
      val expectedOutput = VfsUtil.loadText(outputPatternFile)
      if (expectedOutput.trimEnd('\n') == outputString.trimEnd('\n')) {
        return CheckResult(CheckStatus.Solved, CheckUtils.CONGRATULATIONS)
      }
      val diff = CheckResultDiff(expected = expectedOutput, actual = outputString)
      return CheckResult(CheckStatus.Failed, "Expected output:\n$expectedOutput \nActual output:\n$outputString", diff = diff)
    }
    catch (e: Exception) {
      LOG.error(e)
      return FAILED_TO_CHECK
    }
  }

  companion object {
    const val OUTPUT_PATTERN_NAME = "output.txt"
  }
}