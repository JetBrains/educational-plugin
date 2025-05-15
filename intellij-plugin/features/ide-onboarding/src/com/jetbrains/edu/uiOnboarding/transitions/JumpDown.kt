package com.jetbrains.edu.uiOnboarding.transitions

import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimation
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.JUMP_DURATION
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationStep
import com.jetbrains.edu.uiOnboarding.TransitionType
import java.awt.Point

class JumpDown(data: EduUiOnboardingAnimationData, fromPoint: RelativePoint, toPoint: RelativePoint) : EduUiOnboardingAnimation {
  override val steps: List<EduUiOnboardingAnimationStep> = listOf(
    EduUiOnboardingAnimationStep(data.lookDown, fromPoint, fromPoint, 500),
    EduUiOnboardingAnimationStep(data.jumpDown, fromPoint, toPoint, JUMP_DURATION, Point(0, 60), TransitionType.EASE_OUT),
    EduUiOnboardingAnimationStep(data.lookForward, toPoint, toPoint, 500)
  )

  override val cycle: Boolean = false
}