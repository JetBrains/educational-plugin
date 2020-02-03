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
        is Err -> return result.error
      }

      val outputPatternFile = getOutputFile()
      if (outputPatternFile == null) {
        LOG.warn("Failed to find `output.txt` file (output task `${task.lesson.name}/${task.name}`)")
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

  protected open fun checkOutput(actual: String, expected: String): CheckResult {
    val trimActual = actual.removeLastLineBreaks()
    val trimExpected = expected.removeLastLineBreaks()
    return if (trimActual == trimExpected) {
      CheckResult(CheckStatus.Solved, CheckUtils.CONGRATULATIONS)
    }
    else {
      val diff = CheckResultDiff(expected = trimExpected, actual = trimActual)
      CheckResult(CheckStatus.Failed, "Expected output:\n<$trimExpected>\nActual output:\n<$trimActual>", diff = diff)
    }
  }

  protected fun String.removeLastLineBreaks(): String =
    replace(System.getProperty("line.separator"), "\n").trimEnd('\n')

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