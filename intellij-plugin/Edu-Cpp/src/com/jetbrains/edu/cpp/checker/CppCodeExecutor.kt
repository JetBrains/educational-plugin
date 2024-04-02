package com.jetbrains.edu.cpp.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.CidrTargetRunConfigurationProducer
import com.jetbrains.edu.cpp.codeforces.CppCodeforcesRunConfiguration
import com.jetbrains.edu.learning.checker.DefaultCodeExecutor
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfiguration
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class CppCodeExecutor : DefaultCodeExecutor() {

  override fun createRunConfiguration(project: Project, task: Task): RunnerAndConfigurationSettings? {
    val entryPoint = task.taskFiles
      .mapNotNull { (_, taskFile) ->
        val file = taskFile.getVirtualFile(project)
        if (file == null) {
          LOG.warn("Cannot get a virtual file from the task file '${taskFile.name}'")
        }
        file
      }.firstNotNullOfOrNull { file -> findEntryPointElement(project, file) }

    if (entryPoint == null) {
      return null
    }

    val context = ConfigurationContext(entryPoint)

    val configuration = CidrTargetRunConfigurationProducer.getInstances(project).firstOrNull()?.findOrCreateConfigurationFromContext(context)
    if (configuration == null) {
      LOG.warn("Failed to create a configuration from main function in the file '${entryPoint.containingFile.name}'")
      return null
    }
    return configuration.configurationSettings
  }

  override fun createCodeforcesConfiguration(project: Project, factory: ConfigurationFactory): CodeforcesRunConfiguration {
    return CppCodeforcesRunConfiguration(project, factory)
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(CppCodeExecutor::class.java)
  }
}