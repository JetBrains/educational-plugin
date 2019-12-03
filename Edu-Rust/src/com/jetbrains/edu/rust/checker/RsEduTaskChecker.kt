package com.jetbrains.edu.rust.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicator
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

  override fun computePossibleErrorResult(indicator: ProgressIndicator, stderr: String): CheckResult {
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
    return super.computePossibleErrorResult(indicator, stderr)
  }

  override fun createTestConfigurations(): List<RunnerAndConfigurationSettings> {
    return task.getAllTestDirectories(project).mapNotNull { ConfigurationContext(it).configuration }
  }

  override fun getErrorMessage(node: SMTestProxy): String {
    val message = super.getErrorMessage(node)
    // We assume that Rust plugin should put correct error message into test node
    // or put all test error output into stacktrace
    if (message.isNotEmpty()) return message
    val stacktrace = node.stacktrace.orEmpty()
    val matchResult = ASSERT_MESSAGE_RE.find(stacktrace) ?: return stacktrace
    return matchResult.groups["message"]?.value ?: error("Failed to find `message` capturing group")
  }

  companion object {
    private val ASSERT_MESSAGE_RE =
      """thread '.*' panicked at '(assertion failed: `\(left (.*) right\)`\s*left: `(.*?)`,\s*right: `(.*?)`(: )?)?(?<message>.*)',"""
        .toRegex(setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))
  }
}
