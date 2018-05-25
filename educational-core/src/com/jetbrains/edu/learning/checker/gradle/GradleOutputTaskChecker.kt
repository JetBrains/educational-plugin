package com.jetbrains.edu.learning.checker.gradle

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.checker.*
import com.jetbrains.edu.learning.checker.CheckResult.FAILED_TO_CHECK
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.testDir
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask

class GradleOutputTaskChecker(
  task: OutputTask,
  project: Project,
  private val mainClassForFile: (Project, VirtualFile) -> String?
) : OutputTaskChecker(task, project) {

  override fun check(): CheckResult {
    val result = runGradleRunTask(project, task, mainClassForFile)
    val output = when (result) {
      is Err -> return result.error
      is Ok -> result.value
    }

    val testDir = task.testDir ?: return FAILED_TO_CHECK
    val outputFile = task.getTaskDir(project)
                       ?.findFileByRelativePath(testDir)
                       ?.findChild(OUTPUT_PATTERN_NAME)
                     ?: return FAILED_TO_CHECK

    val expectedOutput = VfsUtil.loadText(outputFile).postProcessOutput()
    if (expectedOutput != output) {
      return CheckResult(CheckStatus.Failed, "Expected output:\n$expectedOutput\nActual output:\n$output")
    }

    return CheckResult(CheckStatus.Solved, TestsOutputParser.CONGRATULATIONS)
  }
}
