package com.jetbrains.edu.uiOnboarding.steps

import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaData

object ZhabaStepFactory {

  fun onboardingStep(id: String): EduUiOnboardingStepAsZhabaStep = EduUiOnboardingStepAsZhabaStep(id)

  fun studentPackPromotionStep(): StudentPackPromotionStep = StudentPackPromotionStep()

  fun <StepData: ZhabaData> noOpStep(stepId: String, outboundTransition: String, dataProvider: (data: EduUiOnboardingAnimationData) -> StepData) =
    NoOpStep(stepId, outboundTransition, dataProvider)

  fun <StepData: ZhabaData> noOpStep(stepId: String, outboundTransition: String, constantData: StepData) =
    noOpStep(stepId, outboundTransition) { constantData }
}