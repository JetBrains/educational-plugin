package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.EditorNotifications


class CCRemoveDependency : CCAnswerPlaceholderAction("Remove Dependency", "Removes dependency on another placeholder") {
  override fun performAnswerPlaceholderAction(state: CCState) {
    val answerPlaceholder = state.answerPlaceholder ?: return
    answerPlaceholder.placeholderDependency = null
    EditorNotifications.getInstance(state.project).updateNotifications(state.file.virtualFile)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val state = getState(e)?: return
    val answerPlaceholder = state.answerPlaceholder ?: return
    e.presentation.isEnabledAndVisible = answerPlaceholder.placeholderDependency != null
  }
}