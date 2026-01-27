package com.jetbrains.edu.uiOnboarding.steps.tour

import com.intellij.openapi.project.Project
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.uiOnboarding.*
import com.jetbrains.edu.uiOnboarding.steps.GotItBalloonStepBase
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep

/**
 * Represents a step for the onboarding tour
 */
abstract class EduUiOnboardingStep internal constructor(override val stepId: String): GotItBalloonStepBase<EduUiOnboardingStepGraphData>() {

  abstract fun buildAnimation(data: EduUiOnboardingAnimationData, point: RelativePoint): EduUiOnboardingAnimation

  protected fun createZhaba(project: Project, data: EduUiOnboardingAnimationData, point: RelativePoint): ZhabaComponent {
    val zhabaComponent = ZhabaComponent(project)
    zhabaComponent.animation = buildAnimation(data, point)
    return zhabaComponent
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

interface EduUiOnboardingStepFactory {
  val stepId: String
  fun create(): EduUiOnboardingStep
}