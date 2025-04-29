package com.jetbrains.edu.ai.debugger.core.listener

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.ai.debugger.core.api.TestFinder
import com.jetbrains.edu.ai.debugger.core.log.AIDebuggerLogEntry
import com.jetbrains.edu.ai.debugger.core.log.logInfo
import com.jetbrains.edu.ai.debugger.core.log.toTaskData
import com.jetbrains.edu.ai.debugger.core.messages.EduAIDebuggerCoreBundle
import com.jetbrains.edu.ai.debugger.core.service.TestInfo
import com.jetbrains.edu.ai.debugger.core.session.AIDebugSessionService
import com.jetbrains.edu.ai.debugger.core.ui.AIDebuggerHintInlineBanner
import com.jetbrains.edu.ai.debugger.core.utils.AIDebugUtils.failedTestName
import com.jetbrains.edu.ai.debugger.core.utils.AIDebugUtils.getInvisibleTestFiles
import com.jetbrains.edu.ai.debugger.core.utils.AIDebugUtils.runWithTests
import com.jetbrains.edu.ai.debugger.core.utils.AIDebugUtils.toNameTextMap
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduTestInfo.Companion.firstFailed
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView

class AIDebuggerCheckListener : CheckListener {
  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    if (!isAvailable(task, result)) return
    val textToShow = EduAIDebuggerCoreBundle.message("action.Educational.AiDebuggerNotification.text")

    val aiDebuggerHintBanner = AIDebuggerHintInlineBanner(textToShow).apply {
      addAction(EduAIDebuggerCoreBundle.message("action.Educational.AiDebuggerNotification.start.debugging.session")) {
        showDebugNotification(task, result) { this.close() }
      }
    }
    TaskToolWindowView.getInstance(project).addInlineBannerToCheckPanel(aiDebuggerHintBanner)
    AIDebuggerLogEntry(
      task = task.toTaskData(),
      actionType = "AIDebuggingNotificationBanner",
      testResult = result,
    ).logInfo()
  }

  private fun showDebugNotification(task: Task, testResult: CheckResult, closeAIDebuggingHint: () -> Unit) {
    val project = task.project ?: error("Project is missing")
    val taskFiles = task.taskFiles.values.filter { it.isVisible }
    if (taskFiles.isEmpty()) return
    val userSolution = taskFiles.toNameTextMap(project)
    val virtualFileMap = runReadAction {
      taskFiles.associate { it.name to (it.getVirtualFile(project) ?: error("Virtual file is not found")) }
    }
    val testName = testResult.failedTestName()
    val testText = runReadAction { TestFinder.findTestByName(project, task, testName) } ?: ""
    val testFiles = runWithTests(project, task, {
      task.getInvisibleTestFiles().toNameTextMap(project)
    }) ?: emptyMap()
    val testInfo = TestInfo(
      errorMessage = testResult.details ?: testResult.message,
      expectedOutput = testResult.diff?.expected ?: "There are no expected output",
      name = testName,
      testFiles = testFiles,
      text = testText
    )
    project.service<AIDebugSessionService>()
      .runDebuggingSession(task, userSolution, virtualFileMap, testResult, testInfo, closeAIDebuggingHint)
    AIDebuggerLogEntry(
      task = task.toTaskData(),
      actionType = "StartDebugSessionIsClicked",
      testResult = testResult,
      testText = testText,
      userCode = userSolution.toString(),
    ).logInfo()
  }

  // TODO: when should we show this button?
  private fun isAvailable(task: Task, result: CheckResult) =
    task.course.courseMode == CourseMode.STUDENT &&
    task.status == CheckStatus.Failed &&
    task is EduTask &&
    result.executedTestsInfo.firstFailed() != null
}
