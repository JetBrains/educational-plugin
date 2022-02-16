package com.jetbrains.edu.php

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.text.nullize
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.runReadActionInSmartMode

class PhpTaskChecker(
  task: EduTask,
  envChecker: EnvironmentChecker,
  project: Project,
) : EduTaskCheckerBase(task, envChecker, project) {

  override fun createDefaultTestConfigurations(): List<RunnerAndConfigurationSettings> {
    return createTestConfigurationsForTestFiles()
  }

  override fun computePossibleErrorResult(indicator: ProgressIndicator, stderr: String): CheckResult {
    val error = getSyntaxError(indicator)
    if (indicator.isCanceled) return CheckResult.CANCELED
    if (error == null) return CheckResult.SOLVED
    return CheckResult(CheckStatus.Failed, message = error)
  }

  private fun getSyntaxError(indicator: ProgressIndicator): String? {
    val configurations = runReadActionInSmartMode(project) { createTestConfigurations() }
    for (configuration in configurations) {
      configuration.isActivateToolWindowBeforeRun = false

      val errorOutput = StringBuilder()
      var destroyed = false
      val processListener = object : ProcessAdapter() {
        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
          val containsError = event.text.replaceFirstChar { it.titlecaseChar() }.contains(error)
          if (outputType == ProcessOutputTypes.STDERR || containsError) {
            errorOutput.append(event.text)
          }
        }

        override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
          super.processWillTerminate(event, willBeDestroyed)
          destroyed = willBeDestroyed
        }
      }

      if (!CheckUtils.executeRunConfigurations(
          project,
          listOf(configuration),
          indicator,
          processListener = processListener)) {
        LOG.warn("Execution was failed while trying to obtain errors for user message")
        destroyed = true
      }

      if (!destroyed) {
        errorOutput.clear()
      }
      return errorOutput.toString().nullize()
    }
    return null
  }

  companion object {
    const val error = "ERROR"
  }
}