package com.jetbrains.edu.uiOnboarding

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject

abstract class ZhabaActionBase : DumbAwareAction() {

  override fun update(e: AnActionEvent) {
    val project = e.project

    e.presentation.isEnabledAndVisible = project != null
                                         && project.isEduProject()
                                         && !EduUiOnboardingService.getInstance(project).tourInProgress
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}