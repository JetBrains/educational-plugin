package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduErrorNotification
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillLoginListener
import com.jetbrains.edu.learning.stepik.hyperskill.metrics.HyperskillMetricsService
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings

class HyperskillCheckListener : CheckListener {

  override fun beforeCheck(project: Project, task: Task) {
    if (task.lesson.course !is HyperskillCourse) {
      return
    }

    HyperskillMetricsService.getInstance().taskStopped()
  }

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    if (task.lesson.course !is HyperskillCourse) {
      return
    }
    // this method is called from EDT, so it means that between if check and if body
    // user cannot navigate between tasks
    require(ApplicationManager.getApplication().isDispatchThread)
    if (result != CheckResult.SOLVED && project.getCurrentTask()?.id == task.id) {
      HyperskillMetricsService.getInstance().taskStarted(task)
    }
    sendSolution(task, project, result)
  }

  private fun sendSolution(
    task: Task,
    project: Project,
    result: CheckResult
  ) {
    if (HyperskillCheckConnector.isRemotelyChecked(task) || task is TheoryTask) {
      /**
       * Solution must be sent after local tests check are made for Edu tasks.
       * Opposite to Edu tasks, e.g., there are no local tests check for Code tasks and code is submitted directly to JBA.
       */
      return
    }

    val course = task.lesson.course as? HyperskillCourse ?: return
    if (!course.isStudy) {
      return
    }

    if (HyperskillSettings.INSTANCE.account == null) {
      EduErrorNotification(
        EduCoreBundle.message("error.failed.to.post.solution.to", EduNames.JBA),
        EduCoreBundle.message("error.login.required", EduNames.JBA),
      ).setListener { notification, e ->
        notification.expire()
        HyperskillLoginListener.hyperlinkUpdate(e)
      }.notify(project)
      return
    }

    if (!isUnitTestMode) {
      ApplicationManager.getApplication().executeOnPooledThread {
        HyperskillCheckConnector.postEduTaskSolution(task, project, result)
      }
    }
    else {
      HyperskillCheckConnector.postEduTaskSolution(task, project, result)
    }
  }
}