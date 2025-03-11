package com.jetbrains.edu.ai.terms.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.edu.ai.terms.TermsLoader
import com.jetbrains.edu.ai.ui.AINotificationManager
import com.jetbrains.edu.learning.ai.terms.TermsProjectSettings
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import org.jetbrains.annotations.NonNls

@Suppress("ComponentNotRegistered")
class UpdateCourseTerms : AITheoryLookupActionBase() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = project.course as? EduCourse ?: return
    if (isActionUnavailable(project, course)) return

    val termsProperties = TermsProjectSettings.getInstance(project).termsProperties.value ?: return
    AINotificationManager.getInstance(project).closeExistingTermsNotifications()
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