package com.jetbrains.edu.uiOnboarding

import com.intellij.ui.awt.RelativePoint
import java.awt.Image
import java.awt.Point

data class EduUiOnboardingAnimationStep(
  val image: Image,
  val fromPoint: RelativePoint,
  val toPoint: RelativePoint,
  val duration: Long, // time in milliseconds
  val imageShift: Point = Point(0, 0)
) {

  val notMoving: Boolean get() =
    fromPoint.component == toPoint.component && fromPoint.point.distanceSq(toPoint.point) < 4
}

interface EduUiOnboardingAnimation {
  val steps: List<EduUiOnboardingAnimationStep>
  val cycle: Boolean
}
