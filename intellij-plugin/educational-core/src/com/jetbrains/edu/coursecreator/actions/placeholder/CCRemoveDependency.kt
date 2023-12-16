package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.actionSystem.Presentation
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

class CCRemoveDependency : CCAnswerPlaceholderAction() {
  override fun performAnswerPlaceholderAction(state: EduState) {
    val answerPlaceholder = state.answerPlaceholder ?: return
    answerPlaceholder.placeholderDependency = null
    YamlFormatSynchronizer.saveItem(state.taskFile.task)
    EditorNotifications.getInstance(state.project).updateNotifications(state.virtualFile)
  }

  override fun updatePresentation(eduState: EduState, presentation: Presentation) {
    val answerPlaceholder = eduState.answerPlaceholder ?: return
    presentation.isEnabledAndVisible = answerPlaceholder.placeholderDependency != null
  }
}
