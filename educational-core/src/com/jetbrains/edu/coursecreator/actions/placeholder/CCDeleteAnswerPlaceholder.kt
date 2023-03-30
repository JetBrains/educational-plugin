package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle.message

class CCDeleteAnswerPlaceholder : CCAnswerPlaceholderAction() {
  override fun performAnswerPlaceholderAction(project: Project, state: EduState) {
    deletePlaceholder(project, state)
  }

  override fun updatePresentation(eduState: EduState, presentation: Presentation) {
    presentation.isEnabledAndVisible = canDeletePlaceholder(eduState)
  }

  companion object {
    const val ACTION_ID: String = "Educational.Educator.DeleteAnswerPlaceholder"
    private fun deletePlaceholder(project: Project, state: EduState) {
      val taskFile = state.taskFile
      val answerPlaceholder = state.answerPlaceholder
                              ?: throw IllegalStateException("Delete Placeholder action called, but no placeholder found")
      EduUtils.runUndoableAction(project, message("action.Educational.Educator.DeleteAnswerPlaceholder.text.full"),
        object : CCAddAnswerPlaceholder.AddAction(project, answerPlaceholder, taskFile, state.editor) {
          override fun undo() {
            super.redo()
          }

          override fun redo() {
            super.undo()
          }
        })
    }

    private fun canDeletePlaceholder(state: EduState): Boolean {
      return if (state.editor.selectionModel.hasSelection()) {
        false
      }
      else {
        state.answerPlaceholder != null
      }
    }
  }
}
