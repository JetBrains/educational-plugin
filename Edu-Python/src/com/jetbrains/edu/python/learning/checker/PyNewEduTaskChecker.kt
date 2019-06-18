package com.jetbrains.edu.python.learning.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.courseFormat.ext.getAllTestFiles
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.python.run.PythonRunConfiguration

class PyNewEduTaskChecker(task: EduTask, project: Project) : EduTaskCheckerBase(task, project) {

  override fun createTestConfigurations(): List<RunnerAndConfigurationSettings> {
    // In general, python plugin can create run configuration for a directory
    // but it can skip some test files if they han't proper names
    return task.getAllTestFiles(project)
      .mapNotNull { ConfigurationContext(it).configuration }
      .filter { it.configuration !is PythonRunConfiguration }
  }
}
