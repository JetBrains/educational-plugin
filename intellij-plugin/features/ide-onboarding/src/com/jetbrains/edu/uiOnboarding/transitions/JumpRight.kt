package com.jetbrains.edu.uiOnboarding.transitions

import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimation
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.JUMP_DURATION
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationStep
import com.jetbrains.edu.uiOnboarding.TransitionType
import java.awt.Point

class JumpRight(data: EduUiOnboardingAnimationData, fromPoint: RelativePoint, toPoint: RelativePoint) : EduUiOnboardingAnimation {
  override val steps: List<EduUiOnboardingAnimationStep> = listOf(
    EduUiOnboardingAnimationStep(data.jumpRight1, fromPoint, fromPoint, 500),
    EduUiOnboardingAnimationStep(data.jumpRight2, fromPoint, toPoint, JUMP_DURATION, Point(16, 80), TransitionType.EASE_OUT),
    EduUiOnboardingAnimationStep(data.lookRight, toPoint, toPoint, 500)
  )

  override val cycle: Boolean = false
}