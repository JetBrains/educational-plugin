package com.jetbrains.edu.learning.codeforces.update

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_TITLE
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CONTEST
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls

@Suppress("ComponentNotRegistered")
class CodeforcesUpdateContestAction : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (project.isDisposed) return

    val course = project.course as? CodeforcesCourse ?: return
    CodeforcesCourseUpdater(project, course).updateCourse { isUpdated ->
      if (!isUpdated) {
        showUpToDateNotification(project)
      }
    }
  }

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    if (!EduUtils.isStudentProject(project)) return

    val course = project.course ?: return
    if (course !is CodeforcesCourse) return

    presentation.isEnabledAndVisible = true
  }

  private fun showUpToDateNotification(project: Project) {
    val notification = Notification(
      "JetBrains Academy",
      "",
      EduCoreBundle.message("update.notification.text", CODEFORCES_TITLE, CONTEST),
      NotificationType.INFORMATION
    )
    notification.notify(project)
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Codeforces.UpdateContest"
  }
}
