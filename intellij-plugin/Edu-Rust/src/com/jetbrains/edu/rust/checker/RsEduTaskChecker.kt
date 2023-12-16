package com.jetbrains.edu.rust.checker

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.rust.messages.EduRustBundle.message
import org.rust.cargo.project.model.cargoProjects
import org.rust.cargo.project.settings.rustSettings
import org.rust.cargo.project.workspace.CargoWorkspace
import org.rust.cargo.runconfig.command.CargoCommandConfiguration
import org.rust.cargo.runconfig.command.CargoCommandConfigurationType
import org.rust.cargo.runconfig.mergeWithDefault
import org.rust.cargo.toolchain.CargoCommandLine
import org.rust.cargo.toolchain.tools.cargo

class RsEduTaskChecker(project: Project, envChecker: EnvironmentChecker, task: EduTask) : EduTaskCheckerBase(task, envChecker, project) {

  override fun computePossibleErrorResult(indicator: ProgressIndicator, stderr: String): CheckResult {
    val pkg = findCargoPackage() ?: return CheckResult(CheckStatus.Failed, message("error.no.package.for.task", task.name))
    // `--color never` is needed to avoid unexpected color escape codes in output
    val cmd = CargoCommandLine.forPackage(pkg, "test", listOf("--no-run", "--color", "never")).copy(emulateTerminal = false)

    val disposable = StudyTaskManager.getInstance(project)
    val cargo = project.rustSettings.toolchain?.cargo() ?: return CheckResult(CheckStatus.Failed, message("error.no.toolchain"))
    val processOutput = cargo.toGeneralCommandLine(project, cmd).executeCargoCommandLine(disposable)
    return RsStderrAnalyzer.tryToGetCheckResult(processOutput.stdout) ?: super.computePossibleErrorResult(indicator, stderr)
  }

  override val preferredConfigurationType: ConfigurationType
    get() = CargoCommandConfigurationType.getInstance()

  override fun createDefaultTestConfigurations(): List<RunnerAndConfigurationSettings> {
    val configurations = createTestConfigurationsForTestDirectories().filter { it.configuration.type == preferredConfigurationType }

    return configurations.ifEmpty {
      val pkg = findCargoPackage() ?: return@ifEmpty emptyList()
      val cmd = CargoCommandLine.forPackage(pkg, "test", listOf("--color", "never")).copy(emulateTerminal = false)

      val configurationSetting = RunManager.getInstance(project)
        .createConfiguration("tests", CargoCommandConfigurationType.getInstance().factory)
      val configuration = configurationSetting.configuration as CargoCommandConfiguration
      cmd.mergeWithDefault(configuration)
      configuration.setFromCmd(cmd)

      listOf(configurationSetting)
    }
  }

  private fun findCargoPackage(): CargoWorkspace.Package? {
    val taskDir = task.getDir(project.courseDir) ?: error("Failed to find directory of `${task.name}` task")
    return runReadAction { project.cargoProjects.findPackageForFile(taskDir) }
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
