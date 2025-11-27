package com.jetbrains.edu.uiOnboarding

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.ZHABA_DIMENSION
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.zhabaScale
import com.jetbrains.edu.uiOnboarding.stepsGraph.JumpingAwayZhabaData
import com.jetbrains.edu.uiOnboarding.stepsGraph.StartZhabaData
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaData
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaDataWithComponent
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep
import com.jetbrains.edu.uiOnboarding.transitions.*
import javax.swing.JFrame

/**
 * A helper class to create a transition between two [ZhabaStep].
 * To decide on the type of transition, [ZhabaData]s are examined.
 */
object TransitionAnimator {

  fun animateTransition(
    project: Project,
    animationData: EduUiOnboardingAnimationData,
    currentData: ZhabaData,
    nextData: ZhabaData
  ): EduUiOnboardingAnimation? {
    val frame = WindowManager.getInstance().getFrame(project) ?: return null

    return when {
      currentData is ZhabaDataWithComponent && nextData is ZhabaDataWithComponent -> {
        val fromPoint = currentData.zhabaPoint
        val toPoint = nextData.zhabaPoint
        animateTransitionBetweenPoints(frame, animationData, fromPoint, toPoint)
      }

      currentData is StartZhabaData && nextData is ZhabaDataWithComponent -> {
        currentData.transitionAnimation(nextData, animationData)
      }

      currentData is ZhabaDataWithComponent && nextData is JumpingAwayZhabaData -> {
        val fromPoint = currentData.zhabaPoint
        FinalJumpDown(nextData.zhabaImage, animationData, fromPoint, frame)
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

    val longEnoughForSideJump = ZHABA_DIMENSION.width / 3
    return when {
      localFromPoint.x + longEnoughForSideJump < localToPoint.x -> JumpRight(animationData, fromPoint, toPoint)
      localFromPoint.x - longEnoughForSideJump > localToPoint.x -> JumpLeft(animationData, fromPoint, toPoint)
      localFromPoint.y < localToPoint.y -> JumpDown(animationData, fromPoint, toPoint)
      else -> JumpUp(animationData, fromPoint, toPoint)
    }
  }
}