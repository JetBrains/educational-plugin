package com.jetbrains.edu.learning.stepik.hyperskill.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.actions.ActionWithProgressIcon
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.stepik.hyperskill.notifyJBAUnauthorized
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import java.util.function.Supplier

abstract class DownloadDatasetActionBase(
  actionText: Supplier<String>,
  processMessage: String,
  private val expectedTaskStatus: CheckStatus
) : ActionWithProgressIcon(actionText), DumbAware {

  init {
    setUpSpinnerPanel(processMessage)
  }

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    if (!project.isStudentProject()) return
    val course = project.course ?: return
    if (!course.isStepikRemote && course !is HyperskillCourse) return

    val task = project.getCurrentTask() as? DataTask ?: return
    if (task.status != expectedTaskStatus) return

    presentation.isEnabledAndVisible = !DownloadDataset.isRunning(project)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  protected fun checkAuthorized(project: Project, course: Course): Boolean {
    when {
      course is HyperskillCourse -> {
        if (HyperskillSettings.INSTANCE.account == null) {
          notifyJBAUnauthorized(project, EduCoreBundle.message("download.dataset.action.is.not.available"))
          return false
        }
      }
      course.isStepikRemote -> {
        if (EduSettings.getInstance().user == null) {
          EduNotificationManager.showErrorNotification(
            project,
            EduCoreBundle.message("download.dataset.action.is.not.available"),
            EduCoreBundle.message("stepik.auth.error.message")
          )
          return false
        }
      }
      else -> return false
    }
    return true
  }

  protected fun downloadDatasetInBackground(project: Project, task: DataTask, submitNewAttempt: Boolean) {
    processStarted()
    val backgroundTask = DownloadDataset(project, task, submitNewAttempt, onFinishedCallback = { processFinished() })
    ProgressManager.getInstance().run(backgroundTask)
  }
}