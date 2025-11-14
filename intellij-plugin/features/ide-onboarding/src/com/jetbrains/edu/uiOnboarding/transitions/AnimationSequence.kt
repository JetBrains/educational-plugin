package com.jetbrains.edu.uiOnboarding.transitions

import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimation
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationStep

/**
 * An animation consisting of multiple animations played in sequence.
 * Their [cycle] value is ignored
 */
class AnimationSequence(
  vararg animations: EduUiOnboardingAnimation,
  override val cycle: Boolean = false
) : EduUiOnboardingAnimation {
  override val steps: List<EduUiOnboardingAnimationStep> = animations.flatMap { it.steps }
}