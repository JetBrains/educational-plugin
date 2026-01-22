package com.jetbrains.edu.ai.translation.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.jetbrains.edu.ai.translation.TRANSLATION_NOTIFICATION_ID
import com.jetbrains.edu.ai.translation.TranslationLoader
import com.jetbrains.edu.ai.translation.ui.CourseTranslationPopup
import com.jetbrains.edu.ai.ui.EducationalAIIcons
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView

class AITranslation : AITranslationActionBase() {

  init {
    templatePresentation.icon = EducationalAIIcons.Translation
    templatePresentation.hoveredIcon = EducationalAIIcons.TranslationHovered
    templatePresentation.selectedIcon = EducationalAIIcons.TranslationPressed
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = project.course as? EduCourse ?: return
    if (isActionUnavailable(project, course)) return

    TaskToolWindowView.getInstance(project).closeExistingTaskDescriptionNotifications(TRANSLATION_NOTIFICATION_ID)

    val popup = CourseTranslationPopup(project, course)
    val relativePoint = JBPopupFactory.getInstance().guessBestPopupLocation(this, e)
    popup.show(relativePoint)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    val course = project.course as? EduCourse ?: return
    if (isActionUnavailable(project, course)) return

    e.presentation.icon = if (TranslationProjectSettings.isCourseTranslated(project)) {
      EducationalAIIcons.TranslationEnabled
    }
    else {
      EducationalAIIcons.Translation
    }

    e.presentation.isEnabledAndVisible = !TranslationLoader.isRunning(project)
  }

  companion object {
    @Suppress("unused")
    const val ACTION_ID: String = "Educational.AITranslation"
  }
}
