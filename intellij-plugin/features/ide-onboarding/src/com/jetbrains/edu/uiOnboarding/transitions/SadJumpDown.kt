package com.jetbrains.edu.uiOnboarding.transitions

import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimation
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.JUMP_DURATION
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationStep
import com.jetbrains.edu.uiOnboarding.TransitionType
import com.jetbrains.edu.uiOnboarding.ZhabaComponent
import java.awt.Point

class SadJumpDown(data: EduUiOnboardingAnimationData, startPoint: RelativePoint, zhabaComponent: ZhabaComponent) : EduUiOnboardingAnimation {

  override val steps: List<EduUiOnboardingAnimationStep> = listOf(
    EduUiOnboardingAnimationStep(data.sad, startPoint, startPoint, 1_000),
    EduUiOnboardingAnimationStep(data.jumpDown, startPoint, pointAtTheBottom(zhabaComponent, startPoint), JUMP_DURATION, Point(0, 10), TransitionType.EASE_OUT),
  )

  override val cycle: Boolean = false
}

internal fun pointAtTheBottom(zhabaComponent: ZhabaComponent, startPoint: RelativePoint) = RelativePoint(
  zhabaComponent,
  Point(startPoint.getPointOn(zhabaComponent).point.x, zhabaComponent.height + EduUiOnboardingAnimationData.zhabaScale(40))
)