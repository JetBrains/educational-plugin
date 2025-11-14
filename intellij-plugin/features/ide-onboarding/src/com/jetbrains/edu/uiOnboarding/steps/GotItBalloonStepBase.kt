package com.jetbrains.edu.uiOnboarding.steps

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.util.Disposer
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.edu.uiOnboarding.GotItBalloonGraphData
import com.jetbrains.edu.uiOnboarding.GotItBalloonStepData
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.RERUN_TRANSITION
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.swing.JComponent

/**
 * A [ZhabaStep] that shows Zhaba with a Got It [com.intellij.openapi.ui.popup.Balloon] component.
 */
abstract class GotItBalloonStepBase<GD: GotItBalloonGraphData>: ZhabaStep<GotItBalloonStepData, GD> {

  abstract fun primaryButtonLabel(graphData: GD): String

  abstract fun isContrastButton(graphData: GD): Boolean

  abstract fun secondaryButtonLabel(graphData: GD): String

  abstract fun onEscape(graphData: GD): String

  abstract fun onPrimaryButton(graphData: GD): String

  abstract fun onSecondaryButton(graphData: GD): String

  override suspend fun executeStep(
    stepData: GotItBalloonStepData,
    graphData: GD,
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
      val transition = onEscape(graphData)
      resume(transition)
    }.requestFocus(true)

    builder.withButtonLabel(primaryButtonLabel(graphData)).withContrastButton(isContrastButton(graphData)).onButtonClick {
      resume(onPrimaryButton(graphData))
    }.withSecondaryButton(secondaryButtonLabel(graphData)) {
      resume(onSecondaryButton(graphData))
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