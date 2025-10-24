package com.jetbrains.edu.uiOnboarding

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.zhabaScale
import com.jetbrains.edu.uiOnboarding.steps.HappyFinishData
import com.jetbrains.edu.uiOnboarding.steps.SadFinishData
import com.jetbrains.edu.uiOnboarding.steps.StartStepData
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaData
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaDataWithComponent
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep
import com.jetbrains.edu.uiOnboarding.transitions.*
import javax.swing.JFrame

/**
 * A helper class to create a transition between two [ZhabaStep].
 * To decide on the type of transition, [ZhabaData]s are examined.
 */
class TransitionAnimator(private val project: Project, private val animationData: EduUiOnboardingAnimationData) {

  fun animateTransition(currentData: ZhabaData, nextData: ZhabaData): EduUiOnboardingAnimation? {
    val frame = WindowManager.getInstance().getFrame(project) ?: return null

    return when {
      currentData is ZhabaDataWithComponent && nextData is ZhabaDataWithComponent -> {
        val fromPoint = currentData.zhabaPoint
        val toPoint = nextData.zhabaPoint
        animateTransitionBetweenPoints(frame, animationData, fromPoint, toPoint)
      }

      currentData is StartStepData && nextData is ZhabaDataWithComponent -> {
        val toPoint = nextData.zhabaPoint
        val animation = BottomToTopAppearance(nextData.zhaba.animation, toPoint)
        animation
      }

      currentData is ZhabaDataWithComponent && nextData is SadFinishData -> {
        val fromPoint = currentData.zhabaPoint
        SadJumpDown(animationData, fromPoint, frame)
      }

      currentData is ZhabaDataWithComponent && nextData is HappyFinishData -> {
        val fromPoint = currentData.zhabaPoint
        HappyJumpDown(animationData, fromPoint, frame)
      }

      else -> null
    }
  }

  /**
   * Move Zhaba from [fromPoint] to [toPoint] smoothly. It could be jumping or movement along a straight line, etc.
   */
  fun animateTransitionBetweenPoints(frame: JFrame, animationData: EduUiOnboardingAnimationData, fromPoint: RelativePoint, toPoint: RelativePoint): EduUiOnboardingAnimation? {
    val localFromPoint = fromPoint.getPoint(frame)
    val localToPoint = toPoint.getPoint(frame)

    val distanceSq = localFromPoint.distanceSq(localToPoint)

    val longEnoughToAnimate = zhabaScale(1)
    if (distanceSq < longEnoughToAnimate * longEnoughToAnimate) return null

    val longEnoughToJump = zhabaScale(100)

    if (distanceSq < longEnoughToJump * longEnoughToJump) {
      return ShortStep(animationData, fromPoint, toPoint, localFromPoint, localToPoint)
    }

    val longEnoughForSideJump = zhabaScale(10)
    return when {
      localFromPoint.x + longEnoughForSideJump < localToPoint.x -> JumpRight(animationData, fromPoint, toPoint)
      localFromPoint.x - longEnoughForSideJump > localToPoint.x -> JumpLeft(animationData, fromPoint, toPoint)
      localFromPoint.y < localToPoint.y -> JumpDown(animationData, fromPoint, toPoint)
      else -> JumpUp(animationData, fromPoint, toPoint)
    }
  }
}