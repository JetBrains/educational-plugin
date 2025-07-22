package com.jetbrains.edu.ai.terms.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.edu.ai.terms.TERMS_NOTIFICATION_ID
import com.jetbrains.edu.ai.terms.TermsLoader
import com.jetbrains.edu.learning.ai.terms.TermsProjectSettings
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import org.jetbrains.annotations.NonNls

class UpdateCourseTerms : AITheoryLookupActionBase() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = project.course as? EduCourse ?: return
    if (isActionUnavailable(project, course)) return

    val termsProperties = TermsProjectSettings.getInstance(project).termsProperties.value ?: return
    TaskToolWindowView.getInstance(project).closeExistingTaskDescriptionNotifications(TERMS_NOTIFICATION_ID)
    TermsLoader.getInstance(project).updateTerms(course, termsProperties)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    val course = project.course as? EduCourse ?: return
    if (isActionUnavailable(project, course)) return
    if (!TermsProjectSettings.areCourseTermsLoaded(project)) return
    e.presentation.isEnabledAndVisible = !TermsLoader.isRunning(project)
  }

  companion object {
    @Suppress("unused")
    @NonNls
    private const val ACTION_ID: String = "Educational.UpdateCourseTerms"
  }
}