package com.jetbrains.edu.go.checker

import com.goide.execution.testing.GoTestRunConfiguration
import com.goide.execution.testing.frameworks.gotest.GotestFramework
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.withRegistryKeyOff

class GoEduTaskChecker(project: Project, envChecker: EnvironmentChecker, task: EduTask) : EduTaskCheckerBase(task, envChecker, project) {
  override fun createDefaultTestConfigurations(): List<RunnerAndConfigurationSettings> {
    return createTestConfigurationsForTestDirectories()
  }

  override fun ConfigurationContext.selectPreferredConfiguration(): RunnerAndConfigurationSettings? {
    return configurationsFromContext?.firstOrNull {
      val configuration = it.configuration as? GoTestRunConfiguration
      configuration?.testFramework is GotestFramework
    }?.configurationSettings
  }

  override fun check(indicator: ProgressIndicator): CheckResult {
    return withRegistryKeyOff(GO_RUN_WITH_PTY) { super.check(indicator) }
  }

  companion object {
    const val GO_RUN_WITH_PTY = "go.run.processes.with.pty"
  }
}
