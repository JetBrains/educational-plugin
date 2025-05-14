package com.jetbrains.edu.ai.debugger.core.session

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerManagerListener
import com.jetbrains.edu.ai.debugger.core.breakpoint.AIBreakPointService
import com.jetbrains.edu.ai.debugger.core.breakpoint.AIBreakPointService.Companion.getAIBreakpointType
import com.jetbrains.edu.ai.debugger.core.breakpoint.AIBreakpointHintMouseMotionListener
import com.jetbrains.edu.ai.debugger.core.utils.AIDebugUtils.failedTestName
import com.jetbrains.edu.ai.debugger.core.utils.AIDebugUtils.getInvisibleTestFiles
import com.jetbrains.edu.ai.debugger.core.utils.AIDebugUtils.runWithTests
import com.jetbrains.edu.learning.checker.CheckUtils.deleteTests
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.ext.getAllTestDirectories
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class AIDebugSessionRunner(
  private val project: Project,
  private val task: Task,
  private val closeAIDebuggingHint: () -> Unit,
  private val listener: AIBreakpointHintMouseMotionListener,
  private val language: Language
) {

  fun runDebuggingSession(testResult: CheckResult) {
    runWithTests(project, task, { startDebugSession(getRunSettingsForFailedTest(testResult)) }, { debugStopped() })
    subscribeToDebuggerEvents()
  }

  private fun debugStopped() {
    deleteTests(task.getInvisibleTestFiles(), project)
    closeAIDebuggingHint()
    AIDebugSessionService.getInstance(project).unlock()
    makeBreakpointsRegular()
    EditorFactory.getInstance().eventMulticaster.apply {
      removeEditorMouseMotionListener(listener)
      removeEditorMouseListener(listener)
    }
  }

  private fun makeBreakpointsRegular() {
    val breakpointManager = XDebuggerManager.getInstance(project).breakpointManager
    breakpointManager.getBreakpoints(language.getAIBreakpointType()).forEach {
      breakpointManager.updateBreakpointPresentation(it, AllIcons.Debugger.Db_set_breakpoint, null)
      project.service<AIBreakPointService>().removeHighlighter(it)
    }
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
    try {
      val debugExecutor = DefaultDebugExecutor.getDebugExecutorInstance()
      val environment = ExecutionEnvironmentBuilder.create(debugExecutor, settings).activeTarget().build()
      ProgramRunner.getRunner(DefaultDebugExecutor.EXECUTOR_ID, settings.configuration)?.execute(environment)
    } catch (e: Exception) {
      LOG.error("Failed to start debug session", e)
    }
  }

  companion object {
    private val LOG: Logger = logger<AIDebugSessionRunner>()
  }
}