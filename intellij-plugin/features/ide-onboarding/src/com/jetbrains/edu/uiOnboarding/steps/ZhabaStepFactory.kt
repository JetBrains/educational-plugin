package com.jetbrains.edu.uiOnboarding.steps

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingBundle
import com.jetbrains.edu.uiOnboarding.steps.tour.CheckSolutionStep
import com.jetbrains.edu.uiOnboarding.steps.tour.CodeEditorStep
import com.jetbrains.edu.uiOnboarding.steps.tour.CourseViewStep
import com.jetbrains.edu.uiOnboarding.steps.tour.EduUiOnboardingStep
import com.jetbrains.edu.uiOnboarding.steps.tour.OnboardingLastStep
import com.jetbrains.edu.uiOnboarding.steps.tour.TaskDescriptionStep
import com.jetbrains.edu.uiOnboarding.steps.tour.WelcomeStep
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaData

object ZhabaStepFactory {

  const val MENU_STEP_ID = ".tode.menu"

  fun onboardingStep(id: String): EduUiOnboardingStep = when (id) {
    WelcomeStep.STEP_KEY -> WelcomeStep()
    TaskDescriptionStep.STEP_KEY -> TaskDescriptionStep()
    CodeEditorStep.STEP_KEY -> CodeEditorStep()
    CheckSolutionStep.STEP_KEY -> CheckSolutionStep()
    CourseViewStep.STEP_KEY -> CourseViewStep()
    else -> throw IllegalArgumentException("Unknown step id: $id")
  }

  fun onboardingLastStep(id: String): OnboardingLastStep = OnboardingLastStep(id)

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