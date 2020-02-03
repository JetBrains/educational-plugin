package com.jetbrains.edu.rust.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.checker.*
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask

class RsOutputTaskChecker(
  project: Project,
  task: OutputTask,
  codeExecutor: CodeExecutor
) : OutputTaskChecker(task, project, codeExecutor) {

  override fun hasCompileError(result: Err<String>): Boolean {
    return result.error.contains(COMPILATION_ERROR_MESSAGE, true)
  }

  override fun checkOutput(actual: String, expected: String): CheckResult {
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
