package com.jetbrains.edu.javascript.learning.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.lang.javascript.ui.NodeModuleNamesUtil
import com.intellij.openapi.project.Project
import com.jetbrains.edu.javascript.learning.installNodeDependencies
import com.jetbrains.edu.javascript.learning.messages.EduJavaScriptBundle
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.checker.tests.TestResultCollector
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.messages.EduFormatBundle

open class JsTaskChecker(task: EduTask, envChecker: EnvironmentChecker, project: Project) : EduTaskCheckerBase(task, envChecker, project) {

  override fun createDefaultTestConfigurations(): List<RunnerAndConfigurationSettings> {
    return createTestConfigurationsForTestFiles()
  }

  override fun validateConfiguration(configuration: RunnerAndConfigurationSettings): CheckResult? {
    return try {
      configuration.checkSettings()
      null
    }
    catch (e: RuntimeConfigurationError) {
      val packageJson = project.courseDir.findChild(NodeModuleNamesUtil.PACKAGE_JSON) ?: return null
      val message = """${EduFormatBundle.message("check.no.tests")}. ${EduJavaScriptBundle.message("install.dependencies")}."""
      CheckResult(CheckStatus.Unchecked, message, hyperlinkAction = { installNodeDependencies(project, packageJson) })
    }
  }

  override fun createTestResultCollector(): TestResultCollector = JsTestResultCollector()
}
