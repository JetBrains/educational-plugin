package com.jetbrains.edu.uiOnboarding.transitions

import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimation
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.JUMP_DURATION
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationStep
import com.jetbrains.edu.uiOnboarding.TransitionType
import java.awt.Image
import java.awt.Point
import javax.swing.JFrame

sealed class FinalJumpDown(image: Image, data: EduUiOnboardingAnimationData, startPoint: RelativePoint, frame: JFrame) : EduUiOnboardingAnimation {

  override val steps: List<EduUiOnboardingAnimationStep> = listOf(
    EduUiOnboardingAnimationStep(image, startPoint, startPoint, 1_000),
    EduUiOnboardingAnimationStep(data.jumpDown, startPoint, pointAtTheBottom(frame, startPoint), JUMP_DURATION, Point(0, 10), TransitionType.EASE_OUT),
  )

  override val cycle: Boolean = false
}

class SadJumpDown(data: EduUiOnboardingAnimationData, startPoint: RelativePoint, frame: JFrame) : FinalJumpDown(
  data.sad, data, startPoint, frame
)

class HappyJumpDown(data: EduUiOnboardingAnimationData, startPoint: RelativePoint, frame: JFrame) : FinalJumpDown(
  data.winking, data, startPoint, frame
)

internal fun pointAtTheBottom(frame: JFrame, startPoint: RelativePoint) = RelativePoint(
  frame,
  Point(startPoint.getPointOn(frame).point.x, frame.height + EduUiOnboardingAnimationData.zhabaScale(40))
)