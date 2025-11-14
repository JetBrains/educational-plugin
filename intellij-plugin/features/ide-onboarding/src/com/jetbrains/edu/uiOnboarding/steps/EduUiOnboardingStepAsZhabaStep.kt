package com.jetbrains.edu.uiOnboarding.steps

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingBundle
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingStep
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingStepGraphData
import com.jetbrains.edu.uiOnboarding.GotItBalloonStepData
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep

/**
 * The implementations of [performStep] is taken from the [com.jetbrains.edu.uiOnboarding.EduUiOnboardingStep] extension point.
 * Only the stepId of the [com.jetbrains.edu.uiOnboarding.EduUiOnboardingStep] is stored to avoid storing an instance managed by the platform.
 */
class EduUiOnboardingStepAsZhabaStep(override val stepId: String): GotItBalloonStepBase<EduUiOnboardingStepGraphData>() {

  private val wrappedStep: EduUiOnboardingStep?
    get() = EduUiOnboardingStep.getIfAvailable(stepId)

  override fun performStep(project: Project, data: EduUiOnboardingAnimationData): GotItBalloonStepData? {
    return wrappedStep?.performStep(project, data)
  }

  override fun isContrastButton(graphData: EduUiOnboardingStepGraphData): Boolean = graphData.isLast

  override fun primaryButtonLabel(graphData: EduUiOnboardingStepGraphData): String = if (!graphData.isLast) {
    EduUiOnboardingBundle.message("gotIt.button.next")
  }
  else {
    EduUiOnboardingBundle.message("gotIt.button.finish")
  }

  override fun secondaryButtonLabel(graphData: EduUiOnboardingStepGraphData): String = if (!graphData.isLast) {
    EduUiOnboardingBundle.message("gotIt.button.skipAll")
  }
  else {
    EduUiOnboardingBundle.message("gotIt.button.restart")
  }

  override fun onEscape(graphData: EduUiOnboardingStepGraphData): String {
    if (graphData.stepIndex != null) {
      EduCounterUsageCollector.uiOnboardingSkipped(graphData.stepIndex, graphData.stepId)
    }

    return if (graphData.isLast) ZhabaStep.HAPPY_FINISH_TRANSITION else ZhabaStep.SAD_FINISH_TRANSITION
  }

  override fun onPrimaryButton(graphData: EduUiOnboardingStepGraphData): String = if (!graphData.isLast) {
    if (graphData.stepIndex == 0) {
      EduCounterUsageCollector.uiOnboardingStarted()
    }
    ZhabaStep.NEXT_TRANSITION
  }
  else {
    EduCounterUsageCollector.uiOnboardingFinished()
    ZhabaStep.HAPPY_FINISH_TRANSITION
  }

  override fun onSecondaryButton(graphData: EduUiOnboardingStepGraphData): String = if (!graphData.isLast) {
    if (graphData.stepIndex != null) {
      EduCounterUsageCollector.uiOnboardingSkipped(graphData.stepIndex, graphData.stepId)
    }
    ZhabaStep.SAD_FINISH_TRANSITION
  }
  else {
    EduCounterUsageCollector.uiOnboardingRelaunched(EduCounterUsageCollector.UiOnboardingRelaunchLocation.TOOLTIP_RESTART_BUTTON)
    ZhabaStep.NEXT_TRANSITION
  }
}