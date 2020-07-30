package com.jetbrains.edu.rust.checker

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckUtils.COMPILATION_FAILED_MESSAGE
import com.jetbrains.edu.learning.checker.CodeExecutor
import com.jetbrains.edu.learning.checker.CodeExecutor.Companion.resultUnchecked
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.findSourceDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.rust.messages.EduRustBundle
import org.rust.cargo.project.settings.rustSettings
import org.rust.cargo.toolchain.CargoCommandLine
import org.rust.lang.RsConstants.MAIN_RS_FILE
import org.rust.lang.core.psi.ext.containingCargoTarget
import org.rust.lang.core.psi.rustFile
import org.rust.openapiext.execute
import org.rust.openapiext.isSuccess

class RsCodeExecutor : CodeExecutor {
  override fun execute(project: Project, task: Task, indicator: ProgressIndicator, input: String?): Result<String, CheckResult> {
    val taskDir = task.getDir(project.courseDir) ?: return resultUnchecked(EduRustBundle.message("error.no.task.dir"))
    val mainVFile = task.findSourceDir(taskDir)?.findChild(MAIN_RS_FILE) ?: return resultUnchecked(EduRustBundle.message("error.failed.find.0", MAIN_RS_FILE))
    val target = runReadAction { PsiManager.getInstance(project).findFile(mainVFile)?.rustFile?.containingCargoTarget }
                 ?: return resultUnchecked(EduRustBundle.message("error.failed.find.target.for.0", MAIN_RS_FILE))
    val cargo = project.rustSettings.toolchain?.rawCargo() ?: return resultUnchecked(EduRustBundle.message("error.no.toolchain"))
    val cmd = CargoCommandLine.forTarget(target, "run")

    val processOutput = cargo.toGeneralCommandLine(project, cmd).execute(project, stdIn = input?.toByteArray())
    val output = processOutput.stdout

    return when {
      processOutput.isSuccess -> Ok(output.prepareToCheck())
      output.contains(COMPILATION_ERROR_MESSAGE, true) ->
        Err(CheckResult(CheckStatus.Failed, COMPILATION_FAILED_MESSAGE, output))
      else -> Err(CheckResult.failedToCheck)
    }
  }

  private fun String.prepareToCheck(): String {
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

    return outputBuffer.toString().removeSuffix("\n")
  }
}