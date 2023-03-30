package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils.isCourseCreator
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.eduState

abstract class CCAnswerPlaceholderAction protected constructor() : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val eduState = getEduState(project) ?: return
    performAnswerPlaceholderAction(project, eduState)
  }

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    val state = getEduState(project)
    state?.let { updatePresentation(it, presentation) }
  }

  protected abstract fun updatePresentation(eduState: EduState, presentation: Presentation)
  protected abstract fun performAnswerPlaceholderAction(project: Project, state: EduState)
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

  private fun getEduState(project: Project): EduState? {
    if (!isCourseCreator(project)) {
      return null
    }
    val state = project.eduState ?: return null
    val lesson = state.taskFile.task.lesson
    // Disable all placeholder actions in non template based framework lessons for now
    return if (lesson is FrameworkLesson && !lesson.isTemplateBased) {
      null
    }
    else state
  }
}
