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

  /**
   * After all steps are executed, should the animation be restarted from the beginning?
   */
  val cycle: Boolean get() = false

  /**
   * If the animation is interrupted, specifies whether it should be stopped immediately, or after all steps are finished.
   *
   * Normally, we do not want to interrupt Zhaba when it moves from one location to another. Because we don't know the intermediate
   * location where it might stop. Such animations with moving are usually not cycled.
   *
   * But if Zhaba stays somewhere without moving, we can interrupt it immediately, and we know its location for sure.
   * Such animations are usually cycled.
   */
  val mayBeInterruptedInsideAnimation: Boolean get() = cycle
}

enum class TransitionType(val f: (Double) -> Double) {
  LINEAR({ t -> t }),
  EASE_OUT({ t -> 1 - (1 - t)*(1 - t) })
}