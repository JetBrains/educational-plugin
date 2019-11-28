package com.jetbrains.edu.rust.checker

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.checker.*
import com.jetbrains.edu.learning.checker.CheckResult.Companion.FAILED_TO_CHECK
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.findTestDirs
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import java.io.IOException

class RsOutputTaskChecker(
  project: Project,
  task: OutputTask,
  codeExecutor: CodeExecutor
) : OutputTaskChecker(task, project, codeExecutor) {

  override fun check(indicator: ProgressIndicator): CheckResult {
    var outputFile: VirtualFile? = null
    for (testDir in task.findTestDirs(project)) {
      outputFile = testDir.findChild(OUTPUT_PATTERN_NAME)
      if (outputFile != null) break
    }

    if (outputFile == null) {
      logWarning("Failed to find `output.txt` file")
      return FAILED_TO_CHECK
    }

    val expectedOutput = try {
      VfsUtil.loadText(outputFile)
    }
    catch (e: IOException) {
      LOG.warn("Failed to read expected output", e)
      return FAILED_TO_CHECK
    }

    val output = when (val result = codeExecutor.execute(project, task)) {
      is Ok -> result.value
      is Err -> {
        return when {
          result.error.contains(COMPILATION_ERROR_MESSAGE, true) -> CheckResult(CheckStatus.Failed, CheckUtils.COMPILATION_FAILED_MESSAGE, result.error)
          else -> {
            logWarning(result.error)
            CheckResult(CheckStatus.Unchecked, result.error)
          }
        }
      }
    }
    return checkOutput(output, expectedOutput)
  }

  private fun logWarning(logMessage: String) {
    LOG.warn("$logMessage (task `${task.lesson.name}/${task.name}`)")
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(RsOutputTaskChecker::class.java)

    private fun checkOutput(actual: String, expected: String): CheckResult {
      var programOutputStarted = false
      val outputBuffer = StringBuilder()
      for (line in actual.lineSequence()) {
        if (programOutputStarted) {
          outputBuffer.appendln(line)
        }
        else {
          if (line.trimStart().startsWith("Running")) {
            programOutputStarted = true
          }
        }
      }

      val output = CheckUtils.postProcessOutput(outputBuffer.toString())
      return if (expected != output) {
        val diff = CheckResultDiff(expected = expected, actual = output)
        CheckResult(CheckStatus.Failed, "Expected output:\n<$expected>\nActual output:\n<$output>", diff = diff)
      }
      else {
        CheckResult(CheckStatus.Solved, CheckUtils.CONGRATULATIONS)
      }
    }
  }
}
