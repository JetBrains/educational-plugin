// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingService

class StartEduUiOnboardingAction : ZhabaActionBase() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    EduUiOnboardingService.getInstance(project).startOnboarding()
  }

  companion object {
    const val ACTION_ID: String = "Educational.StartNewUiOnboardingAction"
  }
}