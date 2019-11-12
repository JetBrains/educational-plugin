package com.jetbrains.edu.rust.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.getAllTestDirectories
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.rust.messages.EduRustBundle.message
import org.rust.cargo.project.model.cargoProjects
import org.rust.cargo.project.settings.rustSettings
import org.rust.cargo.toolchain.CargoCommandLine
import org.rust.openapiext.execute

class RsEduTaskChecker(project: Project, task: EduTask) : EduTaskCheckerBase(task, project) {

  override fun computePossibleErrorResult(stderr: String): CheckResult {
    val taskDir = task.getTaskDir(project) ?: error("Failed to find directory of `${task.name}` task")
    val cargo = project.rustSettings.toolchain?.rawCargo() ?: return CheckResult(CheckStatus.Failed, message("checker.fail.toolchain"))
    val pkg = runReadAction { project.cargoProjects.findPackageForFile(taskDir) } ?:
              return CheckResult(CheckStatus.Failed, message("checker.fail.package", task.name))
    val cmd = CargoCommandLine.forPackage(pkg, "test", listOf("--no-run"))
    val processOutput = cargo.toGeneralCommandLine(project, cmd).execute(project)
    for (line in processOutput.stdoutLines) {
      if (line.trimStart().startsWith(COMPILATION_ERROR_MESSAGE, true)) {
        return CheckResult(CheckStatus.Failed, CheckUtils.COMPILATION_FAILED_MESSAGE, processOutput.stdout)
      }
    }
    return super.computePossibleErrorResult(stderr)
  }

  override fun createTestConfigurations(): List<RunnerAndConfigurationSettings> {
    return task.getAllTestDirectories(project).mapNotNull { ConfigurationContext(it).configuration }
  }

  override fun getErrorMessage(node: SMTestProxy): String {
    val message = super.getErrorMessage(node)
    return if (message.isEmpty()) node.stacktrace.orEmpty() else message
  }
  override fun getComparisonErrorMessage(node: SMTestProxy): String = node.errorMessage
}
