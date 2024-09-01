package com.jetbrains.edu.ai.translation.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.ai.translation.TranslationLoader
import com.jetbrains.edu.ai.translation.dialog.GetCourseTranslationDialog
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import org.jetbrains.annotations.NonNls

class GetCourseTranslation : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (project.isDisposed) return

    val course = project.service<StudyTaskManager>().course as? EduCourse ?: return
    if (!course.isMarketplaceRemote) return

    val selectedLanguage = GetCourseTranslationDialog(course).getLanguage() ?: return
    TranslationLoader.getInstance(project).loadAndSaveWithModalProgress(course, selectedLanguage)

    TaskToolWindowView.getInstance(project).updateTaskDescription()
  }

  override fun update(e: AnActionEvent) {
    val course = e.project?.course as? EduCourse
    e.presentation.isEnabledAndVisible = course?.isMarketplaceRemote == true
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  companion object {
    @Suppress("unused")
    @NonNls
    const val ACTION_ID = "Educational.GetCourseTranslation"
  }
}