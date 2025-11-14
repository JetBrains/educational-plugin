package com.jetbrains.edu.uiOnboarding.transitions

import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimation
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationStep

/**
 * Creates the animation used when the student pack promotion is shown.
 * The "scholarDefault" (smiling Toad with a hat) appears from the bottom to the top, then stays for a while.
 */
object StudentPackAppearance {

  fun create(data: EduUiOnboardingAnimationData, point: RelativePoint): EduUiOnboardingAnimation {
    val helloToad = StudentPackHelloToad(data, point)
    val bottomToTop = BottomToTopAppearance(helloToad, point)

    return AnimationSequence(bottomToTop, helloToad)
  }
}

private class StudentPackHelloToad(data: EduUiOnboardingAnimationData, point: RelativePoint): EduUiOnboardingAnimation {
  override val steps: List<EduUiOnboardingAnimationStep> = listOf(
    EduUiOnboardingAnimationStep(data.scholarDefault, point, point, 2000)
  )

  override val cycle: Boolean = false
}
