package com.jetbrains.edu.rust.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.CodeExecutor
import com.jetbrains.edu.learning.checker.OutputTaskChecker
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask

class RsOutputTaskChecker(
  project: Project,
  task: OutputTask,
  codeExecutor: CodeExecutor
) : OutputTaskChecker(task, project, codeExecutor) {

  override fun String.prepareToCheck(): String {
    var programOutputStarted = false
    val outputBuffer = StringBuilder()

    for (line in lineSequence()) {
      if (programOutputStarted) {
        outputBuffer.appendln(line)
      }
      else {
        if (line.trimStart().startsWith("Running")) {
          programOutputStarted = true
        }
      }
    }

    return CheckUtils.postProcessOutput(outputBuffer.toString())
  }
}
