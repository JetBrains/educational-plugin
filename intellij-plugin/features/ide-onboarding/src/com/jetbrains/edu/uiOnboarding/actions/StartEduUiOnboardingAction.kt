// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.SystemInfo
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.UiOnboardingRelaunchLocation
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingBundle
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaMainGraph

class StartEduUiOnboardingAction : ZhabaActionBase() {

  override val appearanceStepId: String = ZhabaMainGraph.STEP_ID_START_ONBOARDING_JUMP_OUT

  override fun actionPerformed(e: AnActionEvent) {
    super.actionPerformed(e)
    EduCounterUsageCollector.uiOnboardingRelaunched(UiOnboardingRelaunchLocation.MENU_OR_ACTION)
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.text = actionName()
  }

  fun actionName(): String {
    val actionTitle = EduUiOnboardingBundle.message("action.Educational.StartNewUiOnboardingAction.text")

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
    const val ACTION_ID: String = "Educational.PromoteStudentPack"
  }
}