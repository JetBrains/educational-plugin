package com.jetbrains.edu.aiDebugging.core.session

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.*
import com.jetbrains.edu.aiDebugging.core.utils.AIDebugUtils.runWithTests
import com.jetbrains.edu.aiDebugging.core.utils.AIDebugUtils.failedTestName
import com.jetbrains.edu.aiDebugging.core.utils.AIDebugUtils.getInvisibleTestFiles
import com.jetbrains.edu.learning.checker.CheckUtils.deleteTests
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.ext.getAllTestDirectories
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class AIDebugSessionRunner(
  private val project: Project,
  private val task: Task,
  private val closeAIDebuggingHint: () -> Unit
) {

  fun runDebuggingSession(testResult: CheckResult) {
    runWithTests(project, task, { startDebugSession(getRunSettingsForFailedTest(testResult)) }, { debugStopped() })
    subscribeToDebuggerEvents()
  }

  private fun debugStopped() {
    deleteTests(task.getInvisibleTestFiles(), project)
    closeAIDebuggingHint()
    AIDebugSessionService.getInstance(project).unlock()
    // TODO: make breakpoints regular
  }

  private fun subscribeToDebuggerEvents() {
    project.messageBus.connect().subscribe(XDebuggerManager.TOPIC, object : XDebuggerManagerListener {
      override fun processStopped(debugProcess: XDebugProcess) {
        debugStopped()
      }
    })
  }

  private fun getRunSettingsForFailedTest(testResult: CheckResult): RunnerAndConfigurationSettings {
    val methodName = testResult.failedTestName().replace(":", ".")
    val testDirectories = runReadAction { task.getAllTestDirectories(project) }
      .ifEmpty { error("Test directories are not found") }

    val settings = runReadAction { testDirectories.firstNotNullOfOrNull {
      ConfigurationContext(it).configurationsFromContext?.firstOrNull()?.configurationSettings
    } } ?: error("No configuration is found")

    val configuration = settings.configuration
    if (configuration is ExternalSystemRunConfiguration) {
      val settingsData = configuration.settings
      settingsData.taskNames = settingsData.taskNames + listOf("--tests", "\"$methodName\"")
    }
    return settings
  }

  private fun startDebugSession(settings: RunnerAndConfigurationSettings) = runInEdt {
    val environment = ExecutionEnvironmentBuilder.create(DefaultDebugExecutor.getDebugExecutorInstance(), settings).activeTarget().build()
    ProgramRunner.getRunner(DefaultDebugExecutor.EXECUTOR_ID, settings.configuration)?.execute(environment)
  }
}