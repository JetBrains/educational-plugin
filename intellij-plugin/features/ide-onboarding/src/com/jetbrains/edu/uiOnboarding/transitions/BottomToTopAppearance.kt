package com.jetbrains.edu.uiOnboarding.transitions

import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimation
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.zhabaScaleWithoutIDEScale
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationStep
import com.jetbrains.edu.uiOnboarding.TransitionType
import java.awt.Point

class BottomToTopAppearance(nextAnimation: EduUiOnboardingAnimation?, toPoint: RelativePoint) : EduUiOnboardingAnimation {

  override val steps: List<EduUiOnboardingAnimationStep> = listOf(computeStep(nextAnimation, toPoint))

  private fun computeStep(animation: EduUiOnboardingAnimation?, toPoint: RelativePoint): EduUiOnboardingAnimationStep {
    val firstFrame = animation?.steps?.firstOrNull()?.image ?: error("No animation frame on Zhaba appearance")

    val height = zhabaScaleWithoutIDEScale(firstFrame.getHeight(null))
    val shiftedPoint = Point(toPoint.originalPoint.x, toPoint.originalPoint.y + height)

    return EduUiOnboardingAnimationStep(
      firstFrame,
      RelativePoint(toPoint.originalComponent, shiftedPoint),
      toPoint,
      200,
      transitionType = TransitionType.EASE_OUT
    )
  }

  override val cycle: Boolean = false

  /**
   * Zhaba starts moving from this point
   */
  val lowerBoundPoint: RelativePoint
    get() = steps[0].fromPoint
}