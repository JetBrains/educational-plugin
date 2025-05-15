package com.jetbrains.edu.uiOnboarding.transitions

import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimation
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationStep
import java.awt.Point

class JumpLeft(data: EduUiOnboardingAnimationData, fromPoint: RelativePoint, toPoint: RelativePoint) : EduUiOnboardingAnimation {
  override val steps: List<EduUiOnboardingAnimationStep> = listOf(
    EduUiOnboardingAnimationStep(data.jumpLeft1, fromPoint, fromPoint, 1_000),
    EduUiOnboardingAnimationStep(data.jumpLeft2, fromPoint, toPoint, 1_000, Point(0, 100)),
    EduUiOnboardingAnimationStep(data.lookUp, toPoint, toPoint, 1_000)
  )

  override val cycle: Boolean = false
}