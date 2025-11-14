package com.jetbrains.edu.uiOnboarding

import com.intellij.ui.awt.RelativePoint
import java.awt.Rectangle

data class EduUiOnboardingAnimationStep(
  val image: ZhabaImage,
  val fromPoint: RelativePoint,
  val toPoint: RelativePoint,
  val duration: Long, // time in milliseconds
  val transitionType: TransitionType = TransitionType.LINEAR,
  /**
   * Crop bounds relative to the [toPoint]
   */
  val visibleBounds: Rectangle? = null
) {

  val notMoving: Boolean get() =
    fromPoint.component == toPoint.component && fromPoint.point.distanceSq(toPoint.point) < 4
}

interface EduUiOnboardingAnimation {
  val steps: List<EduUiOnboardingAnimationStep>
  val cycle: Boolean
}

enum class TransitionType(val f: (Double) -> Double) {
  LINEAR({ t -> t }),
  EASE_OUT({ t -> 1 - (1 - t)*(1 - t) })
}