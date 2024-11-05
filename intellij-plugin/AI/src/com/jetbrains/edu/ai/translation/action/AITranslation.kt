package com.jetbrains.edu.ai.translation.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.ai.translation.TranslationLoader
import com.jetbrains.edu.ai.translation.dialog.CourseTranslationDialog
import com.jetbrains.edu.ai.ui.EducationalAIIcons
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import com.jetbrains.edu.learning.ai.translationSettings
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse

@Suppress("ComponentNotRegistered")
class AITranslation : DumbAwareAction() {
  init {
    templatePresentation.icon = EducationalAIIcons.Translation
    templatePresentation.hoveredIcon = EducationalAIIcons.TranslationHovered
    templatePresentation.selectedIcon = EducationalAIIcons.TranslationPressed
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (project.isDisposed) return

    val course = project.course as? EduCourse ?: return
    if (!course.isMarketplaceRemote) return

    val dialog = CourseTranslationDialog(project, course)
    if (!dialog.showAndGet()) return
    val selectedLanguage = dialog.getLanguage()
    if (selectedLanguage == null) {
      project.translationSettings().setTranslation(null)
    }
    else {
      TranslationLoader.getInstance(project).fetchAndApplyTranslation(course, selectedLanguage)
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    val course = project.course as? EduCourse ?: return
    if (!course.isStudy || !course.isMarketplaceRemote) {
      return
    }
    e.presentation.icon = if (TranslationProjectSettings.isCourseTranslated(project)) {
      EducationalAIIcons.TranslationEnabled
    }
    else {
      EducationalAIIcons.Translation
    }
    e.presentation.isEnabledAndVisible = !TranslationLoader.isRunning(project)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  companion object {
    @Suppress("unused")
    const val ACTION_ID: String = "Educational.AITranslation"
  }
}
