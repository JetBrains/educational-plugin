package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.actionSystem.Presentation
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.actions.EduActionUtils.runUndoableAction
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import org.jetbrains.annotations.VisibleForTesting

class CCDeleteAnswerPlaceholder : CCAnswerPlaceholderAction() {
  override fun performAnswerPlaceholderAction(state: EduState) {
    val taskFile = state.taskFile
    val answerPlaceholder = state.answerPlaceholder ?: error("Delete Placeholder action called, but no placeholder found")
    runUndoableAction(state.project, message("action.Educational.Educator.DeleteAnswerPlaceholder.text.full"),
      object : CCAddAnswerPlaceholder.AddAction(state.project, answerPlaceholder, taskFile, state.editor) {
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
