package com.jetbrains.edu.python.learning.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.python.testing.AbstractPythonTestRunConfiguration

class PyNewEduTaskChecker(task: EduTask, envChecker: EnvironmentChecker, project: Project) : EduTaskCheckerBase(task, envChecker, project) {

  override fun createDefaultTestConfigurations(): List<RunnerAndConfigurationSettings> {
    // In general, python plugin can create run configuration for a directory,
    // but it can skip some test files if they haven't proper names
    return createTestConfigurationsForTestFiles()
  }

  override fun createTestConfigurationFromPsiElement(element: PsiElement): RunnerAndConfigurationSettings? {
    val configuration = super.createTestConfigurationFromPsiElement(element)
    configuration?.setTaskDirAsWorking()
    return configuration
  }

  override fun ConfigurationContext.selectPreferredConfiguration(): RunnerAndConfigurationSettings? {
    return configuration?.takeIf { it.configuration is AbstractPythonTestRunConfiguration<*> }
  }

  override fun computePossibleErrorResult(indicator: ProgressIndicator, stderr: String): CheckResult =
    PyStderrAnalyzer.tryToGetCheckResult(stderr) ?: CheckResult.SOLVED

  override fun getErrorMessage(node: SMTestProxy): String {
    return node.stacktrace?.lineSequence()?.firstOrNull { it.startsWith(ASSERTION_ERROR) }?.substringAfter(ASSERTION_ERROR)
           ?: node.errorMessage.orEmpty()
  }

  private fun RunnerAndConfigurationSettings.setTaskDirAsWorking() {
    val pythonConfiguration = configuration as? AbstractPythonTestRunConfiguration<*>
    pythonConfiguration?.workingDirectory = task.getDir(project.courseDir)?.path
  }

  companion object {
    private const val ASSERTION_ERROR = "AssertionError: "
  }
}
