package com.jetbrains.edu.python.learning.checker

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.checker.DefaultCodeExecutor
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfiguration
import com.jetbrains.edu.python.learning.codeforces.PyCodeforcesRunConfiguration
import com.jetbrains.python.run.PythonRunConfiguration

class PyCodeExecutor : DefaultCodeExecutor() {
  override fun createRedirectInputConfiguration(project: Project, factory: ConfigurationFactory): CodeforcesRunConfiguration {
    return PyCodeforcesRunConfiguration(project, factory)
  }

  override fun setInputRedirectFile(inputFile: VirtualFile, configuration: RunConfiguration) {
    configuration as? PythonRunConfiguration ?: error("Unable to obtain redirect input options")
    configuration.isRedirectInput = true
    configuration.inputFile = inputFile.path
  }
}
