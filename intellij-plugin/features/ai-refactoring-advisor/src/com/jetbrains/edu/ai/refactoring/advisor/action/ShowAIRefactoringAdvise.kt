package com.jetbrains.edu.ai.refactoring.advisor.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.ai.clippy.assistant.AIClippyService
import com.jetbrains.edu.ai.refactoring.advisor.EduAIRefactoringAdvisorService
import org.jetbrains.annotations.NonNls

@Suppress("ComponentNotRegistered")
class ShowAIRefactoringAdvise : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    EduAIRefactoringAdvisorService.getInstance(project).getClippyComments()
  }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = AIClippyService.isActive(project)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  companion object {
    @Suppress("unused")
    @NonNls
    const val ACTION_ID: String = "Educational.Student.ShowAIRefactoringAdvise"
  }
}