package com.jetbrains.edu.csharp.checker

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.util.application
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.checker.DefaultCodeExecutor
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.rider.model.RunnableProject
import com.jetbrains.rider.run.configurations.project.DotNetProjectConfiguration
import com.jetbrains.rider.run.configurations.project.DotNetProjectConfigurationType
import com.jetbrains.rider.run.configurations.runnableProjectsModelIfAvailable

class CSharpCodeExecutor : DefaultCodeExecutor() {
  private val ansiEscapeDecoder = AnsiEscapeDecoder()

  override fun execute(project: Project, task: Task, indicator: ProgressIndicator, input: String?): Result<String, CheckResult> {
    // Hack: on the first run Rider appends a "\n" to the end of the output, to avoid this, we now run the configuration twice
    // BACKCOMPAT 2025.2: Drop. The issue will be resolved once 252 is dropped, see EDU-8568
    if (application.service<RunConfigurationExecutionCounter>().getAndIncrement() == 0) {
      super.execute(project, task, indicator, input)
    }
    return super.execute(project, task, indicator, input)
  }

  override fun createRunConfiguration(project: Project, task: Task): RunnerAndConfigurationSettings? {
    val runnableProjects = project.runnableProjectsModelIfAvailable?.projects?.valueOrNull
    if (runnableProjects.isNullOrEmpty()) {
      LOG.warn("No runnable .NET projects found in '${project.name}'")
      return null
    }

    val runnableProject = findRunnableProjectForTask(project, task, runnableProjects)
    if (runnableProject == null) {
      LOG.warn("Cannot find runnable project for task '${task.name}'")
      return null
    }

    val runConfiguration = project.service<RunManager>()
      .createConfiguration("Run ${task.name}", DotNetProjectConfigurationType::class.java)

    val dotNetConfig = runConfiguration.configuration as DotNetProjectConfiguration
    dotNetConfig.parameters.projectFilePath = runnableProject.projectFilePath
    dotNetConfig.parameters.projectKind = runnableProject.kind

    return runConfiguration
  }

  /**
   * BACKCOMPAT: 2025.2:
   * use `[dotNetConfig.parameters.terminalMode = TerminalMode.DotNetAllowAnsiColorRedirection]` and drop this function
   * see EDU-8568
   */
  override fun String.applyOutputPostProcessing(): String {
    val chunks = mutableListOf<String>()
    ansiEscapeDecoder.escapeText(this, ProcessOutputTypes.STDOUT) { chunk, _ ->
      chunks.add(chunk)
    }
    // C# outputs Windows line separators (\r\n) regardless of the current OS. "\r\n" is added each time after the first run
    return chunks.joinToString("").removePrefix("\r\n")
  }

  private fun findRunnableProjectForTask(
    project: Project,
    task: Task,
    runnableProjects: List<RunnableProject>
  ): RunnableProject? {
    if (runnableProjects.size == 1) {
      return runnableProjects.first()
    }

    val taskDir = task.getDir(project.courseDir) ?: return null
    val taskPath = taskDir.path
    return runnableProjects.firstOrNull { runnableProject ->
      runnableProject.projectFilePath.startsWith(taskPath)
    }
  }

  companion object {
    private val LOG: Logger = logger<CSharpCodeExecutor>()
  }
}