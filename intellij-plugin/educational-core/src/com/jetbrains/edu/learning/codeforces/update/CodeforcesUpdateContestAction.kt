package com.jetbrains.edu.learning.codeforces.update

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_TITLE
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CONTEST
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import org.jetbrains.annotations.NonNls

@Suppress("ComponentNotRegistered")
class CodeforcesUpdateContestAction : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (project.isDisposed) return

    val course = project.course as? CodeforcesCourse ?: return
    CodeforcesCourseUpdater(project, course).updateCourse { isUpdated ->
      if (!isUpdated) {
        EduNotificationManager.showInfoNotification(
          project,
          content = EduCoreBundle.message("update.notification.text", CODEFORCES_TITLE, CONTEST)
        )
      }
    }
  }

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    if (!project.isStudentProject()) return

    val course = project.course ?: return
    if (course !is CodeforcesCourse) return

    presentation.isEnabledAndVisible = true
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Codeforces.UpdateContest"
  }
}
