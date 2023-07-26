package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.EduUtilsKt.replaceAnswerPlaceholder
import com.jetbrains.edu.learning.eduState
import org.jetbrains.annotations.NonNls

class RefreshAnswerPlaceholder : DumbAwareAction() {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun actionPerformed(e: AnActionEvent) {
    val state = e.eduState ?: return
    val placeholder = state.answerPlaceholder ?: return
    replaceAnswerPlaceholder(state.editor.document, placeholder)
    placeholder.reset(false)
  }

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false
    val state = e.eduState ?: return

    if (!state.task.course.isStudy) {
      presentation.isVisible = true
      return
    }
    if (state.answerPlaceholder != null) {
      presentation.isEnabledAndVisible = true
    }
  }

  companion object {
    const val ACTION_ID: @NonNls String = "Educational.RefreshAnswerPlaceholder"
  }
}
