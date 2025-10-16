package com.jetbrains.edu.csharp.checker

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.jetbrains.edu.csharp.CSharpConfigurator
import com.jetbrains.edu.csharp.allowAnsiCharactersRedirection
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.DefaultCodeExecutor
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.rider.model.RunnableProject
import com.jetbrains.rider.run.configurations.project.DotNetProjectConfiguration
import com.jetbrains.rider.run.configurations.project.DotNetProjectConfigurationType
import com.jetbrains.rider.run.configurations.runnableProjectsModelIfAvailable

class CSharpCodeExecutor : DefaultCodeExecutor() {
  override fun createRunConfiguration(project: Project, task: Task): RunnerAndConfigurationSettings? {
    val customConfiguration = CheckUtils.getCustomRunConfigurationForChecker(project, task)
    if (customConfiguration != null) {
      return customConfiguration
    }

    val mainTaskFile = task.taskFiles.values.firstOrNull { it.name.contains(CSharpConfigurator.TASK_CS) }
    if (mainTaskFile == null) {
      LOG.warn("No C# file found in task '${task.name}'")
      return null
    }

    val virtualFile = mainTaskFile.getVirtualFile(project)
    if (virtualFile == null) {
      LOG.warn("Cannot get virtual file for task file '${mainTaskFile.name}'")
      return null
    }

    val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
    if (psiFile == null) {
      LOG.warn("Cannot get PSI file for '${mainTaskFile.name}'")
      return null
    }
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


    val factory = DotNetProjectConfigurationType().factory
    val runConfiguration = RunManager.getInstance(project).createConfiguration("Run ${psiFile.name}", factory)

    val dotNetConfig = runConfiguration.configuration as DotNetProjectConfiguration
    dotNetConfig.parameters.projectFilePath = runnableProject.projectFilePath
    dotNetConfig.parameters.projectKind = runnableProject.kind

    allowAnsiCharactersRedirection(dotNetConfig)
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