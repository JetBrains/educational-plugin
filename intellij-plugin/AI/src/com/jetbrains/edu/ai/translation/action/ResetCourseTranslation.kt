package com.jetbrains.edu.ai.translation.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.edu.ai.translation.TranslationLoader
import com.jetbrains.edu.ai.ui.AINotificationManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import org.jetbrains.annotations.NonNls

@Suppress("ComponentNotRegistered")
class ResetCourseTranslation : AITranslationActionBase() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = project.course as? EduCourse ?: return
    if (isActionUnavailable(project, course)) return

    AINotificationManager.getInstance(project).closeExistingTranslationNotifications()
    TranslationLoader.getInstance(project).resetCourseTranslation(course)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    val course = project.course as? EduCourse ?: return
    if (isActionUnavailable(project, course)) return
    e.presentation.isEnabledAndVisible = !TranslationLoader.isRunning(project)
  }

  companion object {
    @Suppress("unused")
    @NonNls
    private const val ACTION_ID: String = "Educational.ResetCourseTranslation"
  }
}