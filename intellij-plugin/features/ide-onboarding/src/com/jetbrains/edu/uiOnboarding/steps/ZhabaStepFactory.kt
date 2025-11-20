package com.jetbrains.edu.uiOnboarding.steps

import com.intellij.openapi.actionSystem.ActionGroup
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaData

object ZhabaStepFactory {

  fun onboardingStep(id: String): EduUiOnboardingStepAsZhabaStep = EduUiOnboardingStepAsZhabaStep(id)

  fun studentPackPromotionStep(): StudentPackPromotionStep = StudentPackPromotionStep()

  fun actionGroupStep(stepId: String, actionGroup: ActionGroup): ActionGroupZhabaStep = ActionGroupZhabaStep(stepId, actionGroup)

  fun <StepData: ZhabaData> noOpStep(stepId: String, outboundTransition: String, dataProvider: (data: EduUiOnboardingAnimationData) -> StepData) =
    NoOpStep(stepId, outboundTransition, dataProvider)

  fun <StepData: ZhabaData> noOpStep(stepId: String, outboundTransition: String, constantData: StepData) =
    noOpStep(stepId, outboundTransition, { constantData })
}