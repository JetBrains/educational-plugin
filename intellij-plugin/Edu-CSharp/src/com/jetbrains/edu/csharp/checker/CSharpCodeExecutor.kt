package com.jetbrains.edu.csharp.checker

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.DefaultCodeExecutor
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.rider.model.RunnableProject
import com.jetbrains.rider.run.configurations.TerminalMode
import com.jetbrains.rider.run.configurations.project.DotNetProjectConfiguration
import com.jetbrains.rider.run.configurations.project.DotNetProjectConfigurationType
import com.jetbrains.rider.run.configurations.runnableProjectsModelIfAvailable

class CSharpCodeExecutor : DefaultCodeExecutor() {

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
    dotNetConfig.parameters.terminalMode = TerminalMode.DotNetAllowAnsiColorRedirection

    return runConfiguration
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