package com.jetbrains.edu.ai.debugger.core.listener

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.readText
import com.jetbrains.edu.ai.debugger.core.api.TestFinder
import com.jetbrains.edu.ai.debugger.core.log.AIDebuggerLogEntry
import com.jetbrains.edu.ai.debugger.core.log.logInfo
import com.jetbrains.edu.ai.debugger.core.log.toStringPresentation
import com.jetbrains.edu.ai.debugger.core.log.toTaskData
import com.jetbrains.edu.ai.debugger.core.messages.EduAIDebuggerCoreBundle
import com.jetbrains.edu.ai.debugger.core.session.AIDebugSessionService
import com.jetbrains.edu.ai.debugger.core.ui.AIDebuggerHintInlineBanner
import com.jetbrains.edu.ai.debugger.core.utils.AIDebugUtils.failedTestName
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.EduTestInfo.Companion.firstFailed
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.educational.ml.debugger.dto.TaskDescription
import com.jetbrains.educational.ml.debugger.dto.TaskDescriptionType

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
    val virtualFiles = task.taskFiles.values.filter { it.isVisible }.mapNotNull { it.getVirtualFile(project) }
    if (virtualFiles.isEmpty()) return
    val taskDescription = task.getTaskDescription(project)
    val testText = runReadAction { TestFinder.findTestByName(project, task, testResult.failedTestName()) } ?: ""
    project.service<AIDebugSessionService>()
      .runDebuggingSession(task, taskDescription, virtualFiles, testResult, testText, closeAIDebuggingHint)
    AIDebuggerLogEntry(
      task = task.toTaskData(),
      actionType = "StartDebugSessionIsClicked",
      testResult = testResult,
      testText = testText,
      userCode = virtualFiles.toStringPresentation(),
    ).logInfo()
  }

  // TODO: when should we show this button?
  private fun isAvailable(task: Task, result: CheckResult) =
    task.course.courseMode == CourseMode.STUDENT &&
    task.status == CheckStatus.Failed &&
    task is EduTask &&
    result.executedTestsInfo.firstFailed() != null

  private fun Task.getTaskDescription(project: Project): TaskDescription {
    val description = runReadAction { getDescriptionFile(project)?.readText() } ?: error("There are no description for the task")
    return TaskDescription(description, descriptionFormat.toTaskDescriptionType())
  }

  private fun DescriptionFormat.toTaskDescriptionType() =
    when (this) {
      DescriptionFormat.MD -> TaskDescriptionType.MD
      DescriptionFormat.HTML -> TaskDescriptionType.HTML
    }
}
