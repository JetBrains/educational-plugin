// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.SystemInfo
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.UiOnboardingRelaunchLocation
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingBundle
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingService
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaGraph
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStepBase

abstract class CallTodeActionBase : DumbAwareAction() {

  /**
   * Used when Tode is already on the screen
   */
  protected abstract val zhabaStepId: String

  /**
   * Used when Tode is not on the screen yet, so it should appear first
   */
  protected abstract val zhabaStepWithAppearanceId: String

  @get:NlsActions.ActionText
  protected abstract val actionTitle: String

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project
    if (project == null) {
      return
    }

    EduCounterUsageCollector.uiOnboardingRelaunched(UiOnboardingRelaunchLocation.MENU_OR_ACTION)
    EduUiOnboardingService.getInstance(project).executeZhaba()
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
    fun create(actionTitle: String, zhabaGraph: ZhabaGraph, initialState: ZhabaStepBase): CallTodeActionBase = object : CallTodeActionBase() {
      override fun getGraphAndInitialStep(): Pair<ZhabaGraph, ZhabaStepBase> {
        return zhabaGraph to initialState
      }

      override val actionTitle: String
        get() = actionTitle

    }
  }
}