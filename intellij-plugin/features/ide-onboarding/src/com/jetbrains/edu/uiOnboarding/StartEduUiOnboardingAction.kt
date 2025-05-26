// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.SystemInfo
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.UiOnboardingRelaunchLocation

class StartEduUiOnboardingAction : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project
    if (project == null) {
      return
    }

    EduCounterUsageCollector.uiOnboardingRelaunched(UiOnboardingRelaunchLocation.MENU_OR_ACTION)
    EduUiOnboardingService.getInstance(project).startOnboarding()
  }

  override fun update(e: AnActionEvent) {
    val project = e.project

    e.presentation.text = actionName()

    e.presentation.isEnabledAndVisible = project != null
                                         && project.isEduProject()
                                         && !EduUiOnboardingService.getInstance(project).tourInProgress
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  fun actionName(): String {
    val actionTitle = EduUiOnboardingBundle.message("action.StartNewUiOnboardingAction.text")

    return when {
      SystemInfo.isMac -> {
        // Mac does not show icons in the menu, so we add the icon to the text
        val toadEmoji = EduUiOnboardingBundle.message("toad.emoji")
        "$actionTitle $toadEmoji"
      }
      else -> actionTitle
    }
  }

  companion object {
    const val ACTION_ID: String = "StartNewUiOnboardingAction"
  }
}