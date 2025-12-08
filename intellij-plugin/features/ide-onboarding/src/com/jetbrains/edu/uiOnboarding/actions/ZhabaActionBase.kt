package com.jetbrains.edu.uiOnboarding.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.UiOnboardingRelaunchLocation
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingService

abstract class ZhabaActionBase : DumbAwareAction() {

  /**
   * Step id for which Zhaba appears on the screen.
   */
  protected abstract val appearanceStepId: String

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project
    if (project == null) {
      return
    }

    EduCounterUsageCollector.uiOnboardingRelaunched(UiOnboardingRelaunchLocation.MENU_OR_ACTION)
    EduUiOnboardingService.getInstance(project).executeZhaba(appearanceStepId)
  }

  override fun update(e: AnActionEvent) {
    val project = e.project

    e.presentation.isEnabledAndVisible = project != null
                                         && project.isEduProject()
                                         && !EduUiOnboardingService.getInstance(project).tourInProgress
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}