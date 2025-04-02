package com.jetbrains.edu.javascript.learning.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.javascript.jest.JestRunConfiguration
import com.intellij.lang.javascript.ui.NodeModuleNamesUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement
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

  override fun createTestConfigurationFromPsiElement(element: PsiElement): RunnerAndConfigurationSettings? {
    val configurationSettings = super.createTestConfigurationFromPsiElement(element) ?: return null
    val runConfiguration = configurationSettings.configuration

    // If you want to use ECMAScript modules (https://nodejs.org/api/esm.html) with NodeJS,
    // you have to enable them using `--experimental-vm-modules` option (https://jestjs.io/docs/ecmascript-modules).
    // They are not enabled by default, so it's rather hard to enable them from the user side
    // since run configurations for checking are created automatically.
    // Let's enable them automatically
    if (Registry.`is`("edu.js.ecmascript.modules") && runConfiguration is JestRunConfiguration) {
      runConfiguration.enableECMAScriptModulesSupport()
    }

    return configurationSettings
  }

  private fun JestRunConfiguration.enableECMAScriptModulesSupport() {
    if (EXPERIMENTAL_VM_MODULES !in runSettings.nodeOptions) {
      runSettings = runSettings.modify { builder ->
        builder.nodeOptions = if (builder.nodeOptions.isEmpty()) {
          EXPERIMENTAL_VM_MODULES
        }
        else {
          "${builder.nodeOptions} $EXPERIMENTAL_VM_MODULES"
        }
      }
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

  override fun createTestResultCollector(): TestResultCollector = JsTestResultCollector()

  companion object {
    private const val EXPERIMENTAL_VM_MODULES = "--experimental-vm-modules"
  }
}
