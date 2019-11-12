package com.jetbrains.edu.rust.checker

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckResultDiff
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.OutputTaskChecker
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.findSourceDir
import com.jetbrains.edu.learning.courseFormat.ext.findTestDirs
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import org.rust.cargo.project.settings.rustSettings
import org.rust.cargo.toolchain.CargoCommandLine
import org.rust.lang.RsConstants
import org.rust.lang.core.psi.ext.containingCargoTarget
import org.rust.lang.core.psi.rustFile
import org.rust.openapiext.execute
import org.rust.openapiext.isSuccess
import java.io.IOException

class RsOutputTaskChecker(project: Project, task: OutputTask) : OutputTaskChecker(task, project) {

  override fun check(indicator: ProgressIndicator): CheckResult {
    val taskDir = task.getTaskDir(project) ?: return FailedToCheck("Failed to find task dir")

    var outputFile: VirtualFile? = null
    for (testDir in task.findTestDirs(project)) {
      outputFile = testDir.findChild(OUTPUT_PATTERN_NAME)
      if (outputFile != null) break
    }

    if (outputFile == null) return FailedToCheck("Failed to find `output.txt` file")

    val expectedOutput = try {
      VfsUtil.loadText(outputFile)
    } catch (e: IOException) {
      LOG.warn("Failed to read expected output", e)
      return CheckResult.FAILED_TO_CHECK
    }

    val mainVFile = task.findSourceDir(taskDir)?.findChild(RsConstants.MAIN_RS_FILE)
                    ?: return FailedToCheck("Failed to find `${RsConstants.MAIN_RS_FILE}`")
    val target = runReadAction { PsiManager.getInstance(project).findFile(mainVFile)?.rustFile?.containingCargoTarget }
                 ?: return FailedToCheck("Failed to find target for `${RsConstants.MAIN_RS_FILE}`")
    val cargo = project.rustSettings.toolchain?.rawCargo() ?: return FailedToCheck("Failed to find Rust toolchain")
    val cmd = CargoCommandLine.forTarget(target, "run")

    val processOutput = cargo.toGeneralCommandLine(project, cmd).execute(project)
    val output = processOutput.stdout

    return when {
      processOutput.isSuccess -> checkOutput(output, expectedOutput)
      output.contains(COMPILATION_ERROR_MESSAGE, true) -> CheckResult(CheckStatus.Failed, CheckUtils.COMPILATION_FAILED_MESSAGE, output)
      else -> CheckResult(CheckStatus.Unchecked, output)
    }
  }

  @Suppress("FunctionName")
  private fun FailedToCheck(logMessage: String): CheckResult {
    LOG.warn("$logMessage (task `${task.lesson.name}/${task.name}`)")
    return CheckResult.FAILED_TO_CHECK
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(RsOutputTaskChecker::class.java)

    private fun checkOutput(actual: String, expected: String): CheckResult {
      var programOutputStarted = false
      val outputBuffer = StringBuilder()
      for (line in actual.lineSequence()) {
        if (programOutputStarted) {
          outputBuffer.appendln(line)
        } else {
          if (line.trimStart().startsWith("Running")) {
            programOutputStarted = true
          }
        }
      }

      val output = CheckUtils.postProcessOutput(outputBuffer.toString())
      return if (expected != output) {
        val diff = CheckResultDiff(expected = expected, actual = output)
        CheckResult(CheckStatus.Failed, "Expected output:\n<$expected>\nActual output:\n<$output>", diff = diff)
      } else {
        CheckResult(CheckStatus.Solved, CheckUtils.CONGRATULATIONS)
      }
    }
  }
}
