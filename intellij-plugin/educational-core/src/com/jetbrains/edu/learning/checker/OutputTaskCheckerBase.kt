package com.jetbrains.edu.learning.checker

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckResultDiff
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTaskBase
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.withRegistryKeyOff

abstract class OutputTaskCheckerBase<T: OutputTaskBase>(
  task: T,
  private val envChecker: EnvironmentChecker,
  project: Project,
  private val codeExecutor: CodeExecutor
) : TaskChecker<T>(task, project) {

  protected abstract fun getIncorrectMessage(testFolderName: String): String

  protected abstract fun getTestFolders(project: Project, task: T): Array<out VirtualFile>

  protected abstract fun processCorrectCheckResult(): CheckResult

  protected abstract fun compareOutputs(expected: String, actual: String): Boolean

  protected open fun createLatestOutputFile(testFolder: VirtualFile, actualOutput: String) {}

  override fun check(indicator: ProgressIndicator): CheckResult {
    indicator.text = EduCoreBundle.message("progress.text.output.checker.executing.tests")

    val possibleError = envChecker.getEnvironmentError(project, task)
    if (possibleError != null) {
      return possibleError
    }

    val testFolders = getTestFolders(project, task)

    for ((index, testFolder) in testFolders.withIndex()) {
      val inputVirtualFile = testFolder.findChild(task.inputFileName)
      if (inputVirtualFile == null) LOG.info("No \"input.txt\" file found for task ${task.name}")

      val outputVirtualFile = testFolder.findChild(task.outputFileName)
      if (outputVirtualFile == null) {
        if (inputVirtualFile == null) LOG.info("No \"output.txt\" file found for task ${task.name}")
        continue
      }

      val inputDocument = inputVirtualFile?.let { getDocument(it) }
      val input = runReadAction { inputDocument?.text }

      val outputDocument = getDocument(outputVirtualFile)

      val testNumber = index + 1
      indicator.text2 = EduCoreBundle.message("progress.details.running.test", testNumber, testFolders.size)

      val result = withRegistryKeyOff(RUN_WITH_PTY) {
        codeExecutor.execute(project, task, indicator, input)
      }

      val actualOutput = when (result) {
        is Ok -> CheckUtils.postProcessOutput(result.value)
        is Err -> return result.error
      }
      createLatestOutputFile(testFolder, actualOutput)

      val expectedOutput = runReadAction { outputDocument.text }
      if (compareOutputs(expectedOutput, actualOutput)) {
        val diff = CheckResultDiff(expected = expectedOutput, actual = actualOutput)
        return CheckResult(CheckStatus.Failed, getIncorrectMessage(testFolder.name), diff = diff)
      }
    }
    return processCorrectCheckResult()
  }

  private fun getDocument(virtualFile: VirtualFile): Document {
    return runReadAction { FileDocumentManager.getInstance().getDocument(virtualFile) }
    ?: error("Can't get document of ${virtualFile.name} file - ${virtualFile.path}")
  }

  companion object {
    const val RUN_WITH_PTY = "run.processes.with.pty"
  }
}