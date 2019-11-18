package com.jetbrains.edu.rust.checker

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.ext.findSourceDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.handlers.CodeExecutor
import org.rust.cargo.project.settings.rustSettings
import org.rust.cargo.toolchain.CargoCommandLine
import org.rust.lang.RsConstants.MAIN_RS_FILE
import org.rust.lang.core.psi.ext.containingCargoTarget
import org.rust.lang.core.psi.rustFile
import org.rust.openapiext.execute
import org.rust.openapiext.isSuccess

class RsCodeExecutor : CodeExecutor {
  override fun execute(project: Project, task: Task, input: String?): Result<String, String> {
    val taskDir = task.getTaskDir(project) ?: return Err("Failed to find task dir")
    val mainVFile = task.findSourceDir(taskDir)?.findChild(MAIN_RS_FILE)
                    ?: return Err("Failed to find `$MAIN_RS_FILE`")
    val target = runReadAction { PsiManager.getInstance(project).findFile(mainVFile)?.rustFile?.containingCargoTarget }
                 ?: return Err("Failed to find target for `$MAIN_RS_FILE`")
    val cargo = project.rustSettings.toolchain?.rawCargo() ?: return Err("Failed to find Rust toolchain")
    val cmd = CargoCommandLine.forTarget(target, "run")

    val processOutput = cargo.toGeneralCommandLine(project, cmd).execute(project, stdIn = input?.toByteArray())
    val output = processOutput.stdout

    return when {
      processOutput.isSuccess -> Ok(output)
      else -> Err(output)
    }
  }
}