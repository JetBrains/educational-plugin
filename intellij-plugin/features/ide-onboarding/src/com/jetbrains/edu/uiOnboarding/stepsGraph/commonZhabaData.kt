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

/**
 * In student pack promotion, the appearance animation consists of two steps:
 * - the toad appears from bottom to top
 * - then stays for a while with a smiling face.
 */
object StartStudentPackPromotionZhabaData: StartZhabaData {
  override fun transitionAnimation(nextData: ZhabaDataWithComponent, animationData: EduUiOnboardingAnimationData): EduUiOnboardingAnimation {
    val toPoint = nextData.zhabaPoint

    val helloToadStep = EduUiOnboardingAnimationStep(animationData.scholarDefault, toPoint, toPoint, 2000)
    val bottomToTopStep = BottomToTopAppearance.computeStep(helloToadStep, toPoint)

    return object : EduUiOnboardingAnimation {
      override val steps: List<EduUiOnboardingAnimationStep> = listOf(bottomToTopStep, helloToadStep)
    }
  }
}

data class JumpingAwayZhabaData(val zhabaImage: ZhabaImage) : ZhabaData