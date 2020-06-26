package com.jetbrains.edu.learning.codeforces

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_TITLE
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CONTEST
import com.jetbrains.edu.learning.codeforces.CodeforcesSettings.Companion.codeforcesNotificationGroup
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.messages.EduCoreBundle

@Suppress("ComponentNotRegistered")
class CodeforcesUpdateContestAction : DumbAwareAction(
  EduCoreBundle.message("codeforces.label.update.contest", CodeforcesNames.CODEFORCES_TITLE)) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = project.course ?: return
    if (course !is CodeforcesCourse) {
      showWrongCourseNotification(project)
    }
    if (project.isDisposed || !EduUtils.isStudentProject(project)) return

    CodeforcesCourseUpdater(project, course as CodeforcesCourse).updateCourseAndDoActions(
      onNothingUpdated = { showUpToDateNotification(project) }
    )
  }

  private fun showUpToDateNotification(project: Project) {
    val notification = Notification(
      codeforcesNotificationGroup.displayId,
      "",
      EduCoreBundle.message("update.notification.text", CODEFORCES_TITLE, CONTEST),
      NotificationType.INFORMATION
    )
    notification.notify(project)
  }

  private fun showWrongCourseNotification(project: Project) {
    val notification = Notification(
      codeforcesNotificationGroup.displayId,
      "",
      EduCoreBundle.message("codeforces.update.contest.failed.notification"),
      NotificationType.ERROR
    )
    notification.notify(project)
  }
}