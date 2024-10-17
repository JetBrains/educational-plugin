package com.jetbrains.edu.ai.translation.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.ai.translation.TranslationLoader
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import org.jetbrains.annotations.NonNls

@Suppress("ComponentNotRegistered")
class ResetCourseTranslation : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (project.isDisposed) return

    val course = project.course as? EduCourse ?: return
    if (!course.isMarketplaceRemote) return

    TranslationLoader.getInstance(project).resetCourseTranslation(course)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    val course = project.course as? EduCourse ?: return
    e.presentation.isEnabledAndVisible = course.isMarketplaceRemote == true && !TranslationLoader.isRunning(project)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  companion object {
    @Suppress("unused")
    @NonNls
    private const val ACTION_ID: String = "Educational.ResetCourseTranslation"
  }
}