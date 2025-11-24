package com.jetbrains.edu.uiOnboarding.stepsGraph

import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimation
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationStep
import com.jetbrains.edu.uiOnboarding.ZhabaImage
import com.jetbrains.edu.uiOnboarding.transitions.BottomToTopAppearance

/**
 * Is able to compute the transition animation to the next [ZhabaStep], given its [ZhabaData].
 */
interface StartZhabaData : ZhabaData {
  fun transitionAnimation(nextData: ZhabaDataWithComponent, animationData: EduUiOnboardingAnimationData): EduUiOnboardingAnimation
}

/**
 * In start onboarding, the Toad simply appears from bottom to up.
 */
object StartOnboardingZhabaData: StartZhabaData {
  override fun transitionAnimation(nextData: ZhabaDataWithComponent, animationData: EduUiOnboardingAnimationData): EduUiOnboardingAnimation =
    BottomToTopAppearance(nextData.zhaba.animation, nextData.zhabaPoint)
}

data class JumpingAwayZhabaData(val zhabaImage: ZhabaImage) : ZhabaData