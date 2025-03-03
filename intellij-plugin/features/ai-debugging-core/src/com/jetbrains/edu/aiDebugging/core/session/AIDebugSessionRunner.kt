package com.jetbrains.edu.aiDebugging.core.session

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.util.messages.MessageBusConnection
import com.intellij.xdebugger.*
import com.jetbrains.edu.aiDebugging.core.ui.AIBreakpointHint
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.checker.CheckUtils.createTests
import com.jetbrains.edu.learning.checker.CheckUtils.deleteTests
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.EduTestInfo.Companion.firstFailed
import com.jetbrains.edu.learning.courseFormat.ext.getAllTestDirectories
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.getEditor
import com.jetbrains.educational.ml.ai.debugger.prompt.responses.BreakpointHintsResponse
import com.jetbrains.educational.ml.ai.debugger.prompt.responses.FixCodeForTestResponse

class AIDebugSessionRunner(private val project: Project, private val task: Task, private val closeAIDebuggingHint: () -> Unit) {

  private var breakpointHint: AIBreakpointHint? = null
  private var connection: MessageBusConnection? = null

  fun runDebuggingSession(
    testResult: CheckResult,
    fixes: FixCodeForTestResponse,
    breakpointHints: BreakpointHintsResponse
  ) {
    runWithTests {
      startDebugSession(getRunSettingsForFailedTest(testResult))
    }
    subscribeToDebuggerEvents(fixes, breakpointHints)
  }

  private fun runWithTests(execution: () -> Unit) {
    createTests(task.getInvisibleTestFiles(), project)
    try {
      execution()
    } catch (_: Throwable) {
      debugStopped()
    }
  }

  private fun debugStopped() {
    deleteTests(task.getInvisibleTestFiles(), project)
    closeAIDebuggingHint()
    connection?.disconnect()
    // TODO: make breakpoints regular
  }

  private fun subscribeToDebuggerEvents(
    fixes: FixCodeForTestResponse,
    breakpointHints: BreakpointHintsResponse
  ) {
    connection = project.messageBus.connect()
    connection?.subscribe(XDebuggerManager.TOPIC, object : XDebuggerManagerListener {
      override fun processStopped(debugProcess: XDebugProcess) {
        debugStopped()
      }

      override fun processStarted(debugProcess: XDebugProcess) {
        super.processStarted(debugProcess)
        subscribeToSessionEvents(debugProcess.session, fixes, breakpointHints)
      }
    })
  }

  private fun subscribeToSessionEvents(session: XDebugSession, fixes: FixCodeForTestResponse, breakpointHints: BreakpointHintsResponse) {
    session.addSessionListener(object : XDebugSessionListener {
      override fun sessionPaused() {
        super.sessionPaused()
        val position = session.currentPosition ?: return
        val editor = position.file.getEditor(project) ?: return
        val message = fixes.getHint(position) ?: breakpointHints.getHint(position) ?: error("No breakpoint hint is found")
        breakpointHint?.close()
        breakpointHint = AIBreakpointHint(message, editor, getTextStartOffset(editor, position.line))
      }

      override fun sessionResumed() {
        super.sessionResumed()
        breakpointHint?.close()
      }
      
      private fun FixCodeForTestResponse.getHint(position: XSourcePosition): String? = firstOrNull {
        it.fileName == position.file.name && it.wrongCodeLineNumber == position.line
      }?.breakpointHint
      
      private fun BreakpointHintsResponse.getHint(position: XSourcePosition): String? = content.firstOrNull {
        it.fileName == position.file.name && it.lineNumber == position.line
      }?.hint
    })
  }

  private fun getTextStartOffset(editor: Editor, line: Int): Int {
    val document = editor.document
    val lineStartOffset = document.getLineStartOffset(line)
    val lineText = document.getText(TextRange(lineStartOffset, document.getLineEndOffset(line)))
    return lineStartOffset + (lineText.indexOfFirst { !it.isWhitespace() }.takeIf { it != -1 } ?: 0)
  }

  private fun Task.getInvisibleTestFiles() = taskFiles.values.filter {
    EduUtilsKt.isTestsFile(this, it.name) && !it.isVisible
  }

  private fun getRunSettingsForFailedTest(testResult: CheckResult): RunnerAndConfigurationSettings {
    val methodName = testResult.executedTestsInfo.firstFailed()?.name?.replace(":", ".") ?: error("Method name is not found")
    val settings = runReadAction { task.getAllTestDirectories(project).firstNotNullOfOrNull {
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