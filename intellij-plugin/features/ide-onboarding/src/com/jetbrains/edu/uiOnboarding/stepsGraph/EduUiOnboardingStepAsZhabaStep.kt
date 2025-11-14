package com.jetbrains.edu.uiOnboarding.stepsGraph

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingBundle
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingStep
import com.jetbrains.edu.uiOnboarding.GotItBalloonStepData
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingStepGraphData
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.HAPPY_FINISH_TRANSITION
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.NEXT_TRANSITION
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.RERUN_TRANSITION
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.SAD_FINISH_TRANSITION
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.swing.JComponent

/**
 * A [ZhabaStep] that shows Zhaba with a Got It [com.intellij.openapi.ui.popup.Balloon] component.
 *
 * The implementations of [performStep] is taken from the [EduUiOnboardingStep] extension point.
 * Only the stepId of the [EduUiOnboardingStep] is stored to avoid storing an instance managed by the platform.
 */
class EduUiOnboardingStepAsZhabaStep(override val stepId: String): ZhabaStep<GotItBalloonStepData, EduUiOnboardingStepGraphData> {

  private val wrappedStep: EduUiOnboardingStep?
    get() = EduUiOnboardingStep.getIfAvailable(stepId)

  override fun performStep(project: Project, data: EduUiOnboardingAnimationData): GotItBalloonStepData? {
    return wrappedStep?.performStep(project, data)
  }

  override suspend fun executeStep(
    stepData: GotItBalloonStepData,
    graphData: EduUiOnboardingStepGraphData,
    cs: CoroutineScope,
    disposable: Disposable
  ): String = suspendCancellableCoroutine { continuation ->
    val localDisposable = Disposer.newDisposable(disposable)

    @RequiresEdt
    fun resume(transition: String) {
      Disposer.dispose(localDisposable)
      if (continuation.isActive) {
        continuation.resume(transition) { _, _, _ ->
          //do nothing
        }
      }
    }
    
    continuation.invokeOnCancellation { 
      Disposer.dispose(localDisposable)
    }
    
    cs.launch(Dispatchers.EDT) {
      val success = stepData.zhaba.start()
      if (!success) {
        resume(RERUN_TRANSITION)
      }
    }

    val showInCenter = stepData.position == null
    val builder = stepData.builder

    val ind = graphData.stepIndex ?: 0
    if (ind > 0) {
      builder.withStepNumber("$ind/${graphData.totalSteps}")
    }

    builder.onEscapePressed {
      EduCounterUsageCollector.uiOnboardingSkipped(ind, graphData.stepId)
      val transition = if (graphData.isLast) HAPPY_FINISH_TRANSITION else SAD_FINISH_TRANSITION
      resume(transition)
    }.requestFocus(true)

    if (!graphData.isLast) {
      builder.withButtonLabel(EduUiOnboardingBundle.message("gotIt.button.next")).onButtonClick {
        if (ind == 0) {
          EduCounterUsageCollector.uiOnboardingStarted()
        }
        resume(NEXT_TRANSITION)
      }.withSecondaryButton(EduUiOnboardingBundle.message("gotIt.button.skipAll")) {
        EduCounterUsageCollector.uiOnboardingSkipped(ind, graphData.stepId)
        resume(SAD_FINISH_TRANSITION)
      }
    }
    else {
      builder.withButtonLabel(EduUiOnboardingBundle.message("gotIt.button.finish")).withContrastButton(true).onButtonClick {
        EduCounterUsageCollector.uiOnboardingFinished()
        resume(HAPPY_FINISH_TRANSITION)
      }.withSecondaryButton(EduUiOnboardingBundle.message("gotIt.button.restart")) {
        EduCounterUsageCollector.uiOnboardingRelaunched(EduCounterUsageCollector.UiOnboardingRelaunchLocation.TOOLTIP_RESTART_BUTTON)
        resume(NEXT_TRANSITION)
      }
    }

    val balloon = builder.build(localDisposable) {
      // do not show the pointer if the balloon should be centered
      setShowCallout(!showInCenter)
    }

    if (showInCenter) {
      balloon.showInCenterOf(stepData.tooltipPoint.originalComponent as JComponent)
    }
    else {
      balloon.show(stepData.tooltipPoint, stepData.position)
    }
  }
}