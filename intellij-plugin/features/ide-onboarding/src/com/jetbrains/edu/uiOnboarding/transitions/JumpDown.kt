package com.jetbrains.edu.uiOnboarding.transitions

import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimation
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationStep
import java.awt.Point

class JumpDown(data: EduUiOnboardingAnimationData, fromPoint: RelativePoint, toPoint: RelativePoint) : EduUiOnboardingAnimation {
  override val steps: List<EduUiOnboardingAnimationStep> = listOf(
    EduUiOnboardingAnimationStep(data.lookDown, fromPoint, fromPoint, 1_000),
    EduUiOnboardingAnimationStep(data.jumpDown, fromPoint, toPoint, 500, Point(0, 60)),
    EduUiOnboardingAnimationStep(data.lookForward, toPoint, toPoint, 1_000)
  )

  override val cycle: Boolean = false
}