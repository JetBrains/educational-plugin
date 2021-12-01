package com.jetbrains.edu.learning.stepik.hyperskill.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.actions.ActionWithProgressIcon
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
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
    if (!EduUtils.isStudentProject(project)) return
    if (project.course !is HyperskillCourse) return

    val task = EduUtils.getCurrentTask(project) as? DataTask ?: return
    if (task.status != expectedTaskStatus) return

    presentation.isEnabledAndVisible = !DownloadDataset.isRunning(project)
  }

  protected fun checkAuthorized(project: Project): Boolean {
    return if (HyperskillSettings.INSTANCE.account == null) {
      notifyJBAUnauthorized(project, EduCoreBundle.message("hyperskill.download.dataset.action.is.not.available"))
      false
    }
    else {
      true
    }
  }

  protected fun downloadDatasetInBackground(project: Project, task: DataTask, submitNewAttempt: Boolean) {
    processStarted()
    val backgroundTask = DownloadDataset(project, task, submitNewAttempt, onFinishedCallback = { processFinished() })
    ProgressManager.getInstance().run(backgroundTask)
  }
}