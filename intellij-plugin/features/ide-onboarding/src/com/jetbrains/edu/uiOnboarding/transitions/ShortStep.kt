package com.jetbrains.edu.uiOnboarding.transitions

import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimation
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationStep
import com.jetbrains.edu.uiOnboarding.ZhabaImage
import java.awt.Point
import kotlin.math.abs

class ShortStep(
  data: EduUiOnboardingAnimationData,
  fromPoint: RelativePoint,
  toPoint: RelativePoint,
  localFromPoint: Point,
  localToPoint: Point
) : EduUiOnboardingAnimation {

  override val steps: List<EduUiOnboardingAnimationStep> = listOf(
    EduUiOnboardingAnimationStep(lookImage(data, localFromPoint, localToPoint), fromPoint, toPoint, 500),
  )

  override val cycle: Boolean = false

  companion object {
    private fun lookImage(
      data: EduUiOnboardingAnimationData,
      localFromPoint: Point,
      localToPoint: Point
    ): ZhabaImage {
      val dx = localToPoint.x - localFromPoint.x
      val dy = localToPoint.y - localFromPoint.y

      return when {
        dx < -abs(dy) -> data.lookLeft
        dx > abs(dy) -> data.lookRight
        dy < 0 -> data.lookUp
        else -> data.lookDown
      }
    }
  }
}