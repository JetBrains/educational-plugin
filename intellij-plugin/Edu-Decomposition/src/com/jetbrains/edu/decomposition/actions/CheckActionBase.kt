package com.jetbrains.edu.decomposition.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.decomposition.messages.EduDecompositionBundle
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
  init {
    setUpSpinnerPanel(getActionName())
  }

  abstract val actionAlreadyRunningMessage: String

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    CheckDetailsView.getInstance(project).clear()
    FileDocumentManager.getInstance().saveAllDocuments()
    val task = TaskToolWindowView.getInstance(project).currentTask ?: return
    CheckActionState.getScope(project).launch {
      if (CheckActionState.getInstance(project).doLock()) {
        try {
          if(performCheck(project, task)) {
            checkIsPassed(project, task)
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
    templatePresentation.text = EduDecompositionBundle.message("action.Educational.Check.Completeness.text")
  }

  fun checkIsPassed(project: Project, task: Task) {
    val checkResult = CheckResult(CheckStatus.Solved, EduDecompositionBundle.message("action.check.completeness.success"))
    task.status = checkResult.status
    task.feedback = CheckFeedback(Date(), checkResult)
    TaskToolWindowView.getInstance(project).apply {
      checkFinished(task, checkResult)
      updateCheckPanel(task)
    }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  abstract suspend fun performCheck(project: Project, task: Task): Boolean

  abstract fun getActionName(): String

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