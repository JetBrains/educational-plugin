package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.eduAssistant.ui.NextStepHintNotificationFrame
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent
import javax.swing.JPanel

class AiDebuggingAction : ActionWithProgressIcon(), DumbAware {
  var actionTargetParent: JPanel? = null
  private var aiDebuggingNotificationPanel: JComponent? = null
  private var highlighter: RangeHighlighter? = null

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (DumbService.isDumb(project)) {
      //e.dataContext.showPopup(ActionUtil.getUnavailableMessage(EduCoreBundle.message("action.Educational.AiDebuggingNotification.description"), false))
      return
    }
    FileDocumentManager.getInstance().saveAllDocuments()
    val task = TaskToolWindowView.getInstance(project).currentTask ?: return
    closeNextStepHintNotificationPanel()

    if (!GetDebuggingSessionState.getInstance(project).lock()) {
      //e.dataContext.showPopup(EduCoreBundle.message("action.Educational.AiDebuggingNotification.already.running"))
      return
    }

    GetDebuggingSession(project, task).also {
      ProgressManager.getInstance().run(it)
    }
  }

  private fun closeNextStepHintNotificationPanel() {
    actionTargetParent?.apply {
      //remove(aiDebuggingNotificationPanel)
      revalidate()
      repaint()
    }
    highlighter.remove()
  }

  private fun RangeHighlighter?.remove() = this?.dispose()

  @Suppress("DialogTitleCapitalization")
  private fun showDebugNotification(project: Project) =
    object : AnAction(EduCoreBundle.message("action.Educational.AiDebuggingNotification.start.debugging.session")) {
      override fun actionPerformed(p0: AnActionEvent) {
        highlighter?.dispose()
      }
    }

  private fun rejectHint(task: Task) {
    closeNextStepHintNotificationPanel()
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  private inner class GetDebuggingSession(private val project: Project, private val task: Task) :
    com.intellij.openapi.progress.Task.Backgroundable(project,
      EduCoreBundle.message("action.Educational.AiDebuggingNotification.progress.text"), true) {

    override fun run(indicator: ProgressIndicator) {
      if (!GetDebuggingSessionState.getInstance(project).isLocked) {
        showHintWindow("action.Educational.NextStepHint.error.unlocked")
        return
      }

      processStarted()
      ApplicationManager.getApplication().executeOnPooledThread { EduActionUtils.showFakeProgress(indicator) }

      runBlockingCancellable {

        val action = showDebugNotification(project)
        showHintWindow(EduCoreBundle.message("action.Educational.AiDebuggingNotification.text"), action)
        return@runBlockingCancellable
      }

    }

    override fun onFinished() {
      processFinished()
      GetDebuggingSessionState.getInstance(project).unlock()
    }

    private fun showHintWindow(textToShow: String, action: AnAction? = null) {
      task.status = CheckStatus.Unchecked
      val nextStepHintNotification = NextStepHintNotificationFrame(textToShow, action, actionTargetParent)

      project.invokeLater {
        TaskToolWindowView.getInstance(project).updateCheckPanel(task)
        aiDebuggingNotificationPanel = nextStepHintNotification.rootPane
        aiDebuggingNotificationPanel?.let { actionTargetParent?.add(it, BorderLayout.NORTH) }
      }
    }
  }

  @Service(Service.Level.PROJECT)
  private class GetDebuggingSessionState {
    private val isBusy = AtomicBoolean(false)

    val isLocked: Boolean
      get() = isBusy.get()

    fun lock(): Boolean {
      return isBusy.compareAndSet(false, true)
    }

    fun unlock() {
      isBusy.set(false)
    }

    companion object {
      fun getInstance(project: Project): GetDebuggingSessionState = project.service()
    }
  }

  companion object {

    // Only for the Kotlin Onboarding Introduction: https://plugins.jetbrains.com/plugin/21067-kotlin-onboarding-introduction and for Edu tasks
    fun isNextStepHintApplicable(task: Task) = task.course.id == 21067 && task is EduTask

    fun isAvailable(task: Task) =
      isNextStepHintApplicable(task) && task.course.courseMode == CourseMode.STUDENT && task.status == CheckStatus.Failed // TODO: when should we show this button?

    @NonNls
    const val ACTION_ID = "Educational.AiDebugging"
    //private val HIGHLIGHTER_COLOR = JBColor(0xEFE5FF, 0x433358)
  }
}
