package com.jetbrains.edu.uiOnboarding.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingService

class HideTodeAction : ZhabaActionBase() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    EduUiOnboardingService.getInstance(project).hideTode()
  }

  override fun update(e: AnActionEvent) {
    super.update(e)

    val project = e.project ?: return

    if (!EduUiOnboardingService.getInstance(project).tourInProgress) {
      e.presentation.isEnabledAndVisible = false
    }
  }
}