package com.jetbrains.edu.uiOnboarding.steps

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingBundle
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaData

object ZhabaStepFactory {

  const val MENU_STEP_ID = ".tode.menu"

  fun onboardingStep(id: String): EduUiOnboardingStepAsZhabaStep = EduUiOnboardingStepAsZhabaStep(id)

  fun studentPackPromotionStep(): StudentPackPromotionStep = StudentPackPromotionStep()

  fun menuStep(): ActionGroupZhabaStep = ActionGroupZhabaStep(
    MENU_STEP_ID,
    ActionManager.getInstance().getAction("Educational.TodeActions") as ActionGroup,
    EduUiOnboardingBundle.message("toad.menu.title")
  )

  fun <StepData: ZhabaData> noOpStep(stepId: String, outboundTransition: String, dataProvider: (data: EduUiOnboardingAnimationData) -> StepData) =
    NoOpStep(stepId, outboundTransition, dataProvider)

  fun <StepData: ZhabaData> noOpStep(stepId: String, outboundTransition: String, constantData: StepData) =
    noOpStep(stepId, outboundTransition) { constantData }
}