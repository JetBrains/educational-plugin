package com.jetbrains.edu.javascript.learning.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.lang.javascript.ui.NodeModuleNamesUtil
import com.intellij.openapi.project.Project
import com.jetbrains.edu.javascript.learning.installNodeDependencies
import com.jetbrains.edu.javascript.learning.messages.EduJavaScriptBundle
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.messages.EduFormatBundle

open class JsTaskChecker(task: EduTask, envChecker: EnvironmentChecker, project: Project) : EduTaskCheckerBase(task, envChecker, project) {

  override fun createDefaultTestConfigurations(): List<RunnerAndConfigurationSettings> {
    return createTestConfigurationsForTestFiles()
  }

  // It is tested only with Jest so may not work with other JS test frameworks
  override fun getComparisonErrorMessage(node: SMTestProxy): String = extractComparisonErrorMessage(node)

  override fun getErrorMessage(node: SMTestProxy): String {
    // we suppress the hardcoded string literal inspection here because the failedMessageStart is not visible for users,
    // but is highlighted by the inspection when passed to the function `substringAfter`
    @Suppress("HardCodedStringLiteral")
    val failedMessageStart = "Failed: \""
    val errorMessage = node.errorMessage.orEmpty()
    return if (errorMessage.startsWith(failedMessageStart)) {
      errorMessage.substringAfter(failedMessageStart).substringBeforeLast('"').replace("\\\"", "\"")
    }
    else {
      errorMessage
    }
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
}
