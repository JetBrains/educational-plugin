package com.jetbrains.edu.coursecreator.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import javax.swing.event.HyperlinkEvent

/**
 * Checks all tasks and returns list of failed tasks
 *
 * @return List of failed tasks, null if the indicator was cancelled
 */
fun checkAllTasks(project: Project,
                  course: Course,
                  indicator: ProgressIndicator): List<Task>? {
  val failedTasks = mutableListOf<Task>()
  var curTask = 0
  val tasksNum = getNumberOfTasks(course)
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
    }
    checker.clearState()
  }
  if (indicator.isCanceled) {
    return null
  }
  return failedTasks
}

fun getNumberOfTasks(course: Course): Int {
  var ans = 0
  course.visitTasks { ans++ }
  return ans
}

fun createFailedTasksNotification(failedTasks: List<Task>, tasksNum: Int, project: Project): Notification {
  val notification = Notification(
    "EduTools",
    EduCoreBundle.message("notification.title.check"),
    notificationContent(failedTasks),
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

@Suppress("UnstableApiUsage")
@NlsSafe
private fun notificationContent(failedTasks: List<Task>): String =
  failedTasks.withIndex().joinToString("<br>") {
    "<a href=\"${it.index}\">${it.value.fullName}</a>"
  }

private val Task.fullName: String
  get() = listOfNotNull(lesson.section, lesson, this).joinToString("/") { it.presentableName }