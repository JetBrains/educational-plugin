package com.jetbrains.edu.rust.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.*
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask

class RsOutputTaskChecker(
  project: Project,
  task: OutputTask,
  codeExecutor: CodeExecutor
) : OutputTaskChecker(task, project, codeExecutor) {

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

    val output = outputBuffer.toString().removeLastLineBreaks()
    val trimExpected = expected.removeLastLineBreaks()
    return if (trimExpected != output) {
      val diff = CheckResultDiff(expected = trimExpected, actual = output)
      CheckResult(CheckStatus.Failed, "Expected output:\n<$trimExpected>\nActual output:\n<$output>", diff = diff)
    }
    else {
      CheckResult(CheckStatus.Solved, CheckUtils.CONGRATULATIONS)
    }
  }
}
