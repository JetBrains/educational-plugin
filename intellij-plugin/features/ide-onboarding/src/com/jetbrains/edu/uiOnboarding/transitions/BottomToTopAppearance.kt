package com.jetbrains.edu.uiOnboarding.transitions

import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimation
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationStep
import com.jetbrains.edu.uiOnboarding.TransitionType
import java.awt.Point
import java.awt.Rectangle

class BottomToTopAppearance(nextAnimation: EduUiOnboardingAnimation?, toPoint: RelativePoint) : EduUiOnboardingAnimation {

  override val steps: List<EduUiOnboardingAnimationStep> = listOf(
    computeStep(
      nextAnimation?.steps?.firstOrNull() ?: error("No animation frame on Zhaba appearance"),
      toPoint
    )
  )

  companion object {
    fun computeStep(firstStep: EduUiOnboardingAnimationStep, toPoint: RelativePoint): EduUiOnboardingAnimationStep {
      val firstFrame = firstStep.image

      val imageScreenSize = firstFrame.screenSize

      val shiftedPoint = Point(toPoint.originalPoint.x, toPoint.originalPoint.y + imageScreenSize.height)

      return EduUiOnboardingAnimationStep(
        firstFrame,
        RelativePoint(toPoint.originalComponent, shiftedPoint),
        toPoint,
        200,
        transitionType = TransitionType.EASE_OUT,
        visibleBounds = Rectangle(
          -firstStep.image.screenShiftX,
          -firstStep.image.screenShiftY,
          imageScreenSize.width,
          imageScreenSize.height
        )
      )
    }
  }
}