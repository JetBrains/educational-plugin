package com.jetbrains.edu.decomposition.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.edu.decomposition.messages.EduDecompositionBundle
import com.jetbrains.edu.decomposition.parsers.FunctionParser
import com.jetbrains.edu.learning.EduUtilsKt.showPopup
import com.jetbrains.edu.learning.actions.ActionWithProgressIcon
import com.jetbrains.edu.learning.checker.details.CheckDetailsView
import com.jetbrains.edu.learning.courseFormat.CheckFeedback
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.decomposition.DecompositionStatus
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.selectedTaskFile
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.asJava.classes.runReadAction
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("ComponentNotRegistered")
class CheckCompletenessAction : ActionWithProgressIcon() {

  init {
    setUpSpinnerPanel(EduDecompositionBundle.message("action.check.completeness.in.progress"))
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    CheckDetailsView.getInstance(project).clear()
    FileDocumentManager.getInstance().saveAllDocuments()
    val task = TaskToolWindowView.getInstance(project).currentTask ?: return
    CheckCompletenessActionState.getScope(project).launch {
      if (CheckCompletenessActionState.getInstance(project).doLock()) {
        try {
          processStarted()
          TaskToolWindowView.getInstance(project).checkStarted(task, false)
          performCheckCompletenessTask(project, task)
        } finally {
          CheckCompletenessActionState.getInstance(project).unlock()
          processFinished()
        }
      } else {
        e.dataContext.showPopup(EduDecompositionBundle.message("action.check.completeness.already.running"))
      }
    }
  }

  private suspend fun performCheckCompletenessTask(project: Project, task: Task) {
    withBackgroundProgress(project, EduDecompositionBundle.message("progress.title.checking.completeness"), cancellable = true) {
      val language = task.course.languageById ?: return@withBackgroundProgress
      val files = task.taskFiles.values.filter { it.isVisible }
      val functionNames = runReadAction { FunctionParser.extractFunctionNames(files, project, language) }
      if (functionNames.isEmpty()) return@withBackgroundProgress // TODO
      // TODO: request to ml lib
      if (functionNames.size >= 2) { // TODO: move to success block
        task.decompositionStatus = DecompositionStatus.GRANULARITY_CHECK_NEEDED
        val checkResult = CheckResult(CheckStatus.Solved, EduDecompositionBundle.message("action.check.completeness.success"))
        task.status = checkResult.status
        task.feedback = CheckFeedback(Date(), checkResult)
        TaskToolWindowView.getInstance(project).apply {
          checkFinished(task, checkResult)
          updateCheckPanel(task)
        }
      }
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    project.selectedTaskFile ?: return
    e.presentation.isEnabledAndVisible = !CheckCompletenessActionState.getInstance(project).isLocked
    templatePresentation.text = EduDecompositionBundle.message("action.Educational.Check.Completeness.text")
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  @Service(Service.Level.PROJECT)
  private class CheckCompletenessActionState(private val scope: CoroutineScope) {
    private val isBusy = AtomicBoolean(false)
    fun doLock(): Boolean = isBusy.compareAndSet(false, true)

    val isLocked: Boolean
      get() = isBusy.get()

    fun unlock() {
      isBusy.set(false)
    }

    companion object {
      fun getInstance(project: Project): CheckCompletenessActionState = project.service()

      fun getScope(project: Project) = getInstance(project).scope
    }
  }
}
