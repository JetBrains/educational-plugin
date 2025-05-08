// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject

class StartEduUiOnboardingAction : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project
    if (project == null) {
      return
    }
    EduUiOnboardingService.getInstance(project).startOnboarding()
  }

  override fun update(e: AnActionEvent) {
    val project = e.project
    e.presentation.isEnabledAndVisible = project != null
                                         && project.isEduProject()
                                         && !EduUiOnboardingService.getInstance(project).tourInProgress
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}