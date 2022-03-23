package com.jetbrains.edu.coursecreator.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import org.jetbrains.annotations.NonNls
import javax.swing.event.HyperlinkEvent

class CheckAllTasks : AnAction(EduCoreBundle.lazyMessage("action.check.all.tasks.text")) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return

    ProgressManager.getInstance().run(object : com.intellij.openapi.progress.Task.Backgroundable(
      project,
      EduCoreBundle.message("progress.title.checking.all.tasks"),
      true
    ) {
      override fun run(indicator: ProgressIndicator) {
        val failedTasks = mutableListOf<Task>()
        var curTask = 0
        var tasksNum = 0
        course.visitTasks { tasksNum++ }
        course.visitTasks {
          if (indicator.isCanceled) {
            return@visitTasks
          }
          curTask++
          indicator.fraction = curTask * 1.0 / tasksNum
          val checker = course.configurator?.taskCheckerProvider?.getTaskChecker(it, project)!!
          if (checker is EduTaskCheckerBase) {
            checker.activateRunToolWindow = false
          }
          indicator.text = EduCoreBundle.message("progress.text.checking.task", curTask, tasksNum)
          val checkResult = checker.check(indicator)
          if (checkResult.status != CheckStatus.Solved) {
            failedTasks.add(it)
            LOG.warn("Task ${it.name} ${checkResult.status}: ${checkResult.message}")
          }
          checker.clearState()
        }
        if (indicator.isCanceled) {
          return
        }
        val notification = if (failedTasks.isEmpty()) {
          Notification(
            "EduTools",
            EduCoreBundle.message("notification.title.check.finished"),
            EduCoreBundle.message("notification.content.all.tasks.solved.correctly"),
            NotificationType.INFORMATION
          )
        }
        else {
          createFailedTasksNotification(failedTasks, tasksNum, project)
        }
        Notifications.Bus.notify(notification, project)
      }
    })
  }

  private fun createFailedTasksNotification(failedTasks: MutableList<Task>, tasksNum: Int, project: Project): Notification {
    val notification = Notification(
      "EduTools",
      EduCoreBundle.message("notification.title.check"),
      failedTasks.withIndex().joinToString("<br>") {
        @Suppress("UnstableApiUsage")
        @NlsSafe
        val failedTask = "<a href=\"${it.index}\">${it.value.fullName}</a>"
        failedTask
      },
      NotificationType.WARNING
    )
      .setSubtitle(EduCoreBundle.message("notification.subtitle.some.tasks.failed", failedTasks.size, tasksNum))
      .setListener(object : NotificationListener.Adapter() {
        override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
          notification.hideBalloon()
          NavigationUtils.navigateToTask(project, failedTasks[Integer.valueOf(e.description)])
          EduCounterUsageCollector.taskNavigation(EduCounterUsageCollector.TaskNavigationPlace.CHECK_ALL_NOTIFICATION)
        }
      })

    if (failedTasks.size > 1) {
      notification.addAction(object : AnAction(EduCoreBundle.lazyMessage("action.open.first.failed.task.text")) {
        override fun actionPerformed(e: AnActionEvent) {
          notification.hideBalloon()
          NavigationUtils.navigateToTask(project, failedTasks.first())
          EduCounterUsageCollector.taskNavigation(EduCounterUsageCollector.TaskNavigationPlace.CHECK_ALL_NOTIFICATION)
        }
      })
    }
    return notification
  }

  private val Task.fullName: String
    get() = listOfNotNull(lesson.section, lesson, this).joinToString("/") { it.presentableName }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = CCUtils.isCourseCreator(project)
  }

  companion object {
    private val LOG = Logger.getInstance(CheckAllTasks::class.java)

    @NonNls
    const val ACTION_ID = "Educational.Educator.CheckAllTasks"
  }
}