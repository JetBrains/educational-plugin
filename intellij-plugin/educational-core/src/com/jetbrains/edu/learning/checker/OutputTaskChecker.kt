package com.jetbrains.edu.learning.checker

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.findTestDirs
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.messages.EduCoreBundle

open class OutputTaskChecker(
  task: OutputTask,
  envChecker: EnvironmentChecker,
  project: Project,
  codeExecutor: CodeExecutor
) : OutputTaskCheckerBase<OutputTask>(task, envChecker, project, codeExecutor) {

  override fun getIncorrectMessage(testFolderName: String): String = EduCoreBundle.message("check.incorrect")
  override fun getTestFolders(project: Project, task: OutputTask): Array<out VirtualFile> {
    return task.findTestDirs(project).filter { it.findChild(task.outputFileName) != null }.toTypedArray()
  }

  override fun processCorrectCheckResult(): CheckResult {
    return CheckResult(CheckStatus.Solved, CheckUtils.CONGRATULATIONS)
  }

  override fun compareOutputs(expected: String, actual: String): Boolean {
    return expected != actual
  }
}
