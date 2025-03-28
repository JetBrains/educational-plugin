package com.jetbrains.edu.ai.terms.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.edu.ai.terms.TERMS_NOTIFICATION_ID
import com.jetbrains.edu.ai.terms.TermsLoader
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import org.jetbrains.annotations.NonNls

@Suppress("ComponentNotRegistered")
class ResetCourseTerms : AITheoryLookupActionBase() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = project.course as? EduCourse ?: return
    if (isActionUnavailable(project, course)) return

    TaskToolWindowView.getInstance(project).closeExistingTaskDescriptionNotifications(TERMS_NOTIFICATION_ID)
    TermsLoader.getInstance(project).resetTerms()
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    val course = project.course as? EduCourse ?: return
    if (isActionUnavailable(project, course)) return
    e.presentation.isEnabledAndVisible = !TermsLoader.isRunning(project)
  }

  companion object {
    @Suppress("unused")
    @NonNls
    private const val ACTION_ID: String = "Educational.ResetCourseTerms"
  }
}