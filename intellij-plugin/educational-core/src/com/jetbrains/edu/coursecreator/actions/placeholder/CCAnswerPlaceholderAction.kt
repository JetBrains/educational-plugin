package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.coursecreator.CCUtils.isCourseCreator
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.eduState

abstract class CCAnswerPlaceholderAction : DumbAwareAction() {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun actionPerformed(e: AnActionEvent) {
    val eduState = getEduState(e) ?: return
    performAnswerPlaceholderAction(eduState)
  }

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false
    val state = getEduState(e) ?: return
    updatePresentation(state, presentation)
  }

  protected abstract fun updatePresentation(eduState: EduState, presentation: Presentation)
  protected abstract fun performAnswerPlaceholderAction(state: EduState)

  private fun getEduState(e: AnActionEvent): EduState? {
    val state = e.eduState ?: return null
    if (!isCourseCreator(state.project)) {
      return null
    }
    val lesson = state.taskFile.task.lesson
    // Disable all placeholder actions in non template based framework lessons for now
    return if (lesson is FrameworkLesson && !lesson.isTemplateBased) null else state
  }
}
