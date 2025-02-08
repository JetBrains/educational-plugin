package com.jetbrains.edu.ai.error.explanation.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.ai.error.explanation.ErrorExplanationManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import org.jetbrains.annotations.NonNls

class ShowErrorExplanation : DumbAwareAction() {
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val language = project.course?.languageById ?: return
    ErrorExplanationManager.getInstance(project).getErrorExplanation(language)
  }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = ErrorExplanationManager.getInstance(project).hasPrevStdErr()
  }

  companion object {
    @Suppress("unused")
    @NonNls
    const val ACTION_ID: String = "Educational.Student.ShowErrorExplanation"
  }
}