package com.jetbrains.edu.uiOnboarding.actions

import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingBundle
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaMainGraph

class ZhabaMenuActionGroup(zhabaGraph: ZhabaMainGraph) : DefaultActionGroup(), DumbAware {

  init {
    add(
      CallTodeActionBase.create(
        EduUiOnboardingBundle.message("action.StartNewUiOnboardingAction.text"),
        zhabaGraph,
        zhabaGraph.initialOnboardingStep
      )
    )
    add(
      CallTodeActionBase.create(
        EduUiOnboardingBundle.message("action.PromoteStudentPackAction.text"),
        zhabaGraph,
        zhabaGraph.initialStudentPackPromotionStep
      )
    )
  }
}