package com.jetbrains.edu.go.checker

import com.goide.execution.testing.GoTestRunConfiguration
import com.goide.execution.testing.frameworks.gotest.GotestFramework
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.courseFormat.ext.getAllTestDirectories
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

class GoEduTaskChecker(project: Project, task: EduTask) : EduTaskCheckerBase(task, project) {
  override fun createTestConfigurations(): List<RunnerAndConfigurationSettings> {
    return task.getAllTestDirectories(project).flatMap { getTestConfiguration(it) }
  }

  private fun getTestConfiguration(directory: PsiDirectory): List<RunnerAndConfigurationSettings> {
    return ConfigurationContext(directory).configurationsFromContext
      ?.filter {
        val configuration = it.configuration as? GoTestRunConfiguration
        configuration?.testFramework is GotestFramework
      }?.mapNotNull { it.configurationSettings }.orEmpty()
  }
}
