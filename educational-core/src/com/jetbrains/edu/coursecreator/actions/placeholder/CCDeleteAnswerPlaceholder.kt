package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.actions.runUndoableAction
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import org.jetbrains.annotations.VisibleForTesting

class CCDeleteAnswerPlaceholder : CCAnswerPlaceholderAction() {
  override fun performAnswerPlaceholderAction(project: Project, state: EduState) {
    val taskFile = state.taskFile
    val answerPlaceholder = state.answerPlaceholder
                            ?: throw IllegalStateException("Delete Placeholder action called, but no placeholder found")
    runUndoableAction(project, message("action.Educational.Educator.DeleteAnswerPlaceholder.text.full"),
      object : CCAddAnswerPlaceholder.AddAction(project, answerPlaceholder, taskFile, state.editor) {
        override fun undo() {
          super.redo()
        }

        override fun redo() {
          super.undo()
        }
      })
  }

  override fun updatePresentation(eduState: EduState, presentation: Presentation) {
    if (eduState.editor.selectionModel.hasSelection()) {
      presentation.isEnabledAndVisible = false
    }
    else {
      presentation.isEnabledAndVisible = eduState.answerPlaceholder != null
    }
  }

  companion object {
    @VisibleForTesting
    const val ACTION_ID: String = "Educational.Educator.DeleteAnswerPlaceholder"
  }
}
