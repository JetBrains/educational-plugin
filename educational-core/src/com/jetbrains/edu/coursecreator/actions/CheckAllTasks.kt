package com.jetbrains.edu.coursecreator.actions

import com.google.common.annotations.VisibleForTesting
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.navigation.NavigationUtils
import javax.swing.event.HyperlinkEvent

class CheckAllTasks : AnAction("Check All Tasks") {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return

    ProgressManager.getInstance().run(object : com.intellij.openapi.progress.Task.Backgroundable(project, "Checking all tasks...", true) {
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
          indicator.text = "Checking task $curTask/$tasksNum"
          if (checker.check(indicator).status != CheckStatus.Solved) {
            failedTasks.add(it)
          }
          checker.clearState()
        }
        if (indicator.isCanceled) {
          return
        }
        val notification = if (failedTasks.isEmpty()) {
          Notification(GROUP_ID, "Check Finished", SUCCESS_MESSAGE, NotificationType.INFORMATION)
        }
        else {
          createFailedTasksNotification(failedTasks, tasksNum, project)
        }
        Notifications.Bus.notify(notification, project)
      }
    })
  }

  private fun createFailedTasksNotification(failedTasks: MutableList<Task>, tasksNum: Int, project: Project): Notification {
    val subtitle = "${failedTasks.size} of $tasksNum tasks failed"
    val tasksList = failedTasks.withIndex().joinToString("<br>") { "<a href=\"${it.index}\">${it.value.fullName}</a>" }
    val notification = Notification(GROUP_ID, null, "Check", subtitle, tasksList, NotificationType.WARNING,
                                    object : NotificationListener.Adapter() {
                                      override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
                                        notification.hideBalloon()
                                        NavigationUtils.navigateToTask(project, failedTasks[Integer.valueOf(e.description)])
                                      }
                                    })

    if (failedTasks.size > 1) {
      notification.addAction(object : AnAction("Open First Failed Task") {
        override fun actionPerformed(e: AnActionEvent) {
          notification.hideBalloon()
          NavigationUtils.navigateToTask(project, failedTasks.first())
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
    @VisibleForTesting
    const val SUCCESS_MESSAGE = "All tasks are solved correctly"

    private const val GROUP_ID = "Education: all tasks check"
  }
}