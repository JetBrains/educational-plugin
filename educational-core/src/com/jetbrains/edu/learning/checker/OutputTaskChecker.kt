package com.jetbrains.edu.learning.checker

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.checker.CheckResult.Companion.failedToCheck
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.findTestDirs
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.messages.EduCoreBundle


open class OutputTaskChecker(
  task: OutputTask,
  private val envChecker: EnvironmentChecker,
  project: Project,
  private val codeExecutor: CodeExecutor
) : TaskChecker<OutputTask>(task, project) {

  /**
   * This method contains logic of testing output task that should be same in all implementations.
   * @see checkOutput
   * @see CodeExecutor
   */
  final override fun check(indicator: ProgressIndicator): CheckResult {
    try {
      val possibleError = envChecker.checkEnvironment(project)
      if (possibleError != null) return CheckResult(CheckStatus.Unchecked, possibleError)

      val outputString = when (val result = codeExecutor.execute(project, task, indicator)) {
        is Ok -> CheckUtils.postProcessOutput(result.value)
        is Err -> return result.error
      }

      val outputPatternFile = getOutputFile()
      if (outputPatternFile == null) {
        LOG.warn("Failed to find `output.txt` file (output task `${task.lesson.name}/${task.name}`)")
        return failedToCheck
      }
      val expectedOutput = VfsUtil.loadText(outputPatternFile)
      return checkOutput(outputString, expectedOutput)
    }
    catch (e: Exception) {
      LOG.error(e)
      return failedToCheck
    }
  }

  private fun checkOutput(actual: String, expected: String): CheckResult {
    return if (actual == expected) {
      CheckResult(CheckStatus.Solved, CheckUtils.CONGRATULATIONS)
    }
    else {
      val diff = CheckResultDiff(expected = expected, actual = actual)
      CheckResult(CheckStatus.Failed, EduCoreBundle.message("check.incorrect"), diff = diff)
    }
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