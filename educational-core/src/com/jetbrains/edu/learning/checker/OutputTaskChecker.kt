package com.jetbrains.edu.learning.checker

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.checker.CheckResult.Companion.FAILED_TO_CHECK
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.findTestDirs
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask


open class OutputTaskChecker(
  task: OutputTask,
  project: Project,
  private val codeExecutor: CodeExecutor
) : TaskChecker<OutputTask>(task, project) {

  override fun check(indicator: ProgressIndicator): CheckResult {
    try {
      val outputString = when (val result = codeExecutor.execute(project, task, indicator)) {
        is Ok -> result.value
        is Err -> return when {
          hasCompileError(result) -> CheckResult(CheckStatus.Failed, CheckUtils.COMPILATION_FAILED_MESSAGE, result.error)
          else -> {
            logWarning(result.error)
            CheckResult(CheckStatus.Unchecked, result.error)
          }
        }
      }

      val outputPatternFile = getOutputFile()
      if (outputPatternFile == null) {
        logWarning("Failed to find `output.txt` file")
        return FAILED_TO_CHECK
      }
      val expectedOutput = VfsUtil.loadText(outputPatternFile)
      return checkOutput(outputString, expectedOutput)
    }
    catch (e: Exception) {
      LOG.error(e)
      return FAILED_TO_CHECK
    }
  }

  protected open fun hasCompileError(result: Err<String>): Boolean = false

  protected open fun checkOutput(actual: String, expected: String): CheckResult = when {
    compareResults(actual, expected) -> CheckResult(CheckStatus.Solved, CheckUtils.CONGRATULATIONS)
    else -> {
      val diff = CheckResultDiff(expected = expected, actual = actual)
      CheckResult(CheckStatus.Failed, "Expected output:\n$expected \nActual output:\n$actual", diff = diff)
    }
  }

  protected open fun compareResults(actual: String, expected: String): Boolean {
    return CheckUtils.postProcessOutput(actual) == CheckUtils.postProcessOutput(expected)
  }

  private fun logWarning(logMessage: String) {
    LOG.warn("$logMessage (output task `${task.lesson.name}/${task.name}`)")
  }

  private fun getOutputFile(): VirtualFile? {
    val outputFile = task.findTestDirs(project)
      .mapNotNull { it.findChild(OUTPUT_PATTERN_NAME) }
      .firstOrNull()
    return outputFile ?: task.getTaskDir(project)?.findChild(OUTPUT_PATTERN_NAME)
  }

  companion object {
    const val OUTPUT_PATTERN_NAME = "output.txt"
  }
}