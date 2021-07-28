package com.jetbrains.edu.learning.codeforces.checker

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.checker.*
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.withRegistryKeyOff

class CodeforcesTaskChecker(
  task: CodeforcesTask,
  private val envChecker: EnvironmentChecker,
  project: Project,
  private val codeExecutor: CodeExecutor
) : TaskChecker<CodeforcesTask>(task, project) {

  override fun check(indicator: ProgressIndicator): CheckResult {
    indicator.text = EduCoreBundle.message("progress.text.codeforces.executing.tests")
    val testFolders = task.getTestFolders(project)

    for ((index, testFolder) in testFolders.withIndex()) {
      val inputVirtualFile = testFolder.findChild(task.inputFileName) ?: continue
      val outputVirtualFile = testFolder.findChild(task.outputFileName) ?: continue

      val inputDocument = runReadAction { FileDocumentManager.getInstance().getDocument(inputVirtualFile) }
                          ?: error("Can't get document of input file - ${inputVirtualFile.path}")
      val outputDocument = runReadAction { FileDocumentManager.getInstance().getDocument(outputVirtualFile) }
                           ?: error("Can't get document of output file - ${outputVirtualFile.path}")

      val testNumber = index + 1
      indicator.text2 = EduCoreBundle.message("progress.details.codeforces.running.test", testNumber, testFolders.size)

      val input = runReadAction { inputDocument.text }

      val possibleError = envChecker.getEnvironmentError(project, task)
      if (possibleError != null) {
        return possibleError
      }

      val result = withRegistryKeyOff(RUN_WITH_PTY) {
        codeExecutor.execute(project, task, indicator, input)
      }

      val output = when (result) {
        is Ok -> result.value.trimEnd('\n')
        is Err -> return result.error
      }
      GeneratorUtils.createChildFile(project, testFolder, task.latestOutputFileName, output.trimEnd('\n'))

      val expectedOutput = runReadAction { outputDocument.text }.trimEnd('\n')
      if (expectedOutput != output) {
        val message = EduCoreBundle.message("codeforces.test.failed", testFolder.name)
        val diff = CheckResultDiff(expected = expectedOutput, actual = output)
        return CheckResult(CheckStatus.Failed, message, diff = diff)
      }
    }
    /**
     * Message will be added in
     * @see com.jetbrains.edu.learning.taskDescription.ui.check.CheckDetailsPanel.createCodeforcesSuccessMessagePanel
     * It's better not to reuse hyperlinkListener mechanism.
     *
     * We need to check the status of Codeforces task in checker tests, e.g.
     * @see com.jetbrains.edu.python.slow.checker.PyCodeforcesCheckerTest and similar
     */
    return if (isUnitTestMode) CheckResult.SOLVED else CheckResult.UNCHECKED
  }

  companion object {
    private const val RUN_WITH_PTY = "run.processes.with.pty"
  }
}
