package com.jetbrains.edu.decomposition.actions

import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.edu.learning.EduUtilsKt.showPopup
import com.jetbrains.edu.learning.actions.ActionWithProgressIcon
import com.jetbrains.edu.learning.checker.details.CheckDetailsView
import com.jetbrains.edu.learning.courseFormat.CheckFeedback
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.selectedTaskFile
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

abstract class CheckActionBase: ActionWithProgressIcon() {

  abstract val actionAlreadyRunningMessage: String

  abstract val templatePresentationMessage: String

  abstract val checkFailureMessage: String

  abstract val checkingProgressTitle: String

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    CheckDetailsView.getInstance(project).clear()
    FileDocumentManager.getInstance().saveAllDocuments()
    val task = TaskToolWindowView.getInstance(project).currentTask ?: return
    CheckActionState.getScope(project).launch {
      if (CheckActionState.getInstance(project).doLock()) {
        try {
          val isCheckSuccessful = withBackgroundProgress(project, checkingProgressTitle, cancellable = true) {
            performCheck(project, task)
          }
          if (!isCheckSuccessful) {
            checkIsFailed(project, task)
          }
        } finally {
          CheckActionState.getInstance(project).unlock()
        }
      } else {
        e.dataContext.showPopup(actionAlreadyRunningMessage)
      }
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    project.selectedTaskFile ?: return
    e.presentation.isEnabledAndVisible = !CheckActionState.getInstance(project).isLocked
    templatePresentation.text = templatePresentationMessage
  }

  protected fun invokeNextAction(action: ActionWithProgressIcon, project: Project) {
    runInEdt {
      ActionUtil.invokeAction(
        action,
        AnActionEvent.createEvent(action, SimpleDataContext.getProjectContext(project), null, "", ActionUiKind.NONE, null),
        null
      )
    }
  }

  private fun checkIsFailed(project: Project, task: Task) {
    val checkResult = CheckResult(CheckStatus.Failed, checkFailureMessage)
    task.status = checkResult.status
    task.feedback = CheckFeedback(Date(), checkResult)
    TaskToolWindowView.getInstance(project).apply {
      checkFinished(task, checkResult)
      updateCheckPanel(task)
    }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  abstract suspend fun performCheck(project: Project, task: Task): Boolean

  @Service(Service.Level.PROJECT)
  private class CheckActionState(private val scope: CoroutineScope) {
    private val isBusy = AtomicBoolean(false)
    fun doLock(): Boolean = isBusy.compareAndSet(false, true)

    val isLocked: Boolean
      get() = isBusy.get()

    fun unlock() {
      isBusy.set(false)
    }

    companion object {
      fun getInstance(project: Project): CheckActionState = project.service()

      fun getScope(project: Project) = getInstance(project).scope
    }
  }
}
