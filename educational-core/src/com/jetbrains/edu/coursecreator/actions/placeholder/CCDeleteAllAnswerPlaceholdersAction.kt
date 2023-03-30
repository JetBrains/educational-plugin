package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.PlaceholderPainter.hidePlaceholders
import com.jetbrains.edu.learning.PlaceholderPainter.showPlaceholders
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.messages.EduCoreBundle.message

class CCDeleteAllAnswerPlaceholdersAction : CCAnswerPlaceholderAction() {
  override fun updatePresentation(eduState: EduState, presentation: Presentation) {
    presentation.isEnabledAndVisible = eduState.taskFile.answerPlaceholders.size > 1
  }

  override fun performAnswerPlaceholderAction(project: Project, state: EduState) {
    val action = ClearPlaceholders(project, state.taskFile, state.editor)
    EduUtils.runUndoableAction(project, message("action.Educational.Educator.DeleteAllPlaceholders.text"), action, UndoConfirmationPolicy.REQUEST_CONFIRMATION)
  }

  private class ClearPlaceholders(project: Project, taskFile: TaskFile, editor: Editor) : TaskFileUndoableAction(
    project,
    taskFile,
    editor
  ) {
    private val placeholders: List<AnswerPlaceholder>

    init {
      placeholders = ArrayList(taskFile.answerPlaceholders)
    }

    override fun performUndo(): Boolean {
      placeholders.forEach {
        taskFile.addAnswerPlaceholder(it)
      }
      showPlaceholders(project, taskFile)
      return true
    }

    override fun performRedo() {
      hidePlaceholders(taskFile)
      taskFile.removeAllPlaceholders()
    }

    override fun isGlobal(): Boolean = true
  }

  companion object {
    const val ACTION_ID = "Educational.Educator.DeleteAllPlaceholders"
  }
}
