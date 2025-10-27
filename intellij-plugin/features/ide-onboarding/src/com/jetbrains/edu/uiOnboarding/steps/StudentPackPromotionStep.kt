package com.jetbrains.edu.uiOnboarding.steps

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.ui.GotItComponentBuilder
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimation
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.EYE_SHIFT
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.SMALL_SHIFT
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.ZHABA_DIMENSION
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationStep
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingBundle
import com.jetbrains.edu.uiOnboarding.GotItBalloonStepData
import com.jetbrains.edu.uiOnboarding.ZhabaComponent
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.FINISH_TRANSITION
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.SAD_FINISH_TRANSITION
import java.awt.Point

class StudentPackPromotionStep : GotItBalloonStepBase<GotItBalloonGraphData>() {

  private fun buildAnimation(data: EduUiOnboardingAnimationData, point: RelativePoint): EduUiOnboardingAnimation =
    object : EduUiOnboardingAnimation {
      override val steps: List<EduUiOnboardingAnimationStep> = listOfNotNull(
        EduUiOnboardingAnimationStep(data.scholarWinking, point, point, 3_000, imageShift = Point(0, 41)),
      )

      override val cycle: Boolean = true
    }

  override fun performStep(
    project: Project,
    data: EduUiOnboardingAnimationData
  ): GotItBalloonStepData? {
    val projectViewToolWindow = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
                                  .getToolWindow("Project") ?: return null
    projectViewToolWindow.show()

    val component = projectViewToolWindow.component

    // Position of the zhaba on the project view component
    val zhabaDimension = ZHABA_DIMENSION
    val zhabaPoint = Point(
      (component.width - zhabaDimension.width) / 2,
      component.height - zhabaDimension.height
    )
    val relativeZhabaPoint = RelativePoint(component, zhabaPoint)
    val zhabaComponent = ZhabaComponent(project)
    zhabaComponent.animation = buildAnimation(data, relativeZhabaPoint)

    // Position the balloon at the bottom of the project view component
    val point = Point(zhabaPoint.x + EYE_SHIFT, zhabaPoint.y - SMALL_SHIFT)
    val relativePoint = RelativePoint(component, point)

    val builder = GotItComponentBuilder { EduUiOnboardingBundle.message("student.pack.promotion.text") }
      .withHeader(EduUiOnboardingBundle.message("student.pack.promotion.title"))

    return GotItBalloonStepData(builder, relativePoint, relativeZhabaPoint, Balloon.Position.above, zhabaComponent)
  }

  override fun isContrastButton(graphData: GotItBalloonGraphData): Boolean = false

  override fun primaryButtonLabel(graphData: GotItBalloonGraphData): String = EduUiOnboardingBundle.message("student.pack.promotion.apply.now")

  override fun secondaryButtonLabel(graphData: GotItBalloonGraphData): String = EduUiOnboardingBundle.message("student.pack.promotion.not.a.student")

  override fun onEscape(graphData: GotItBalloonGraphData): String = SAD_FINISH_TRANSITION

  override fun onPrimaryButton(graphData: GotItBalloonGraphData): String {
    return FINISH_TRANSITION
  }

  override fun onSecondaryButton(graphData: GotItBalloonGraphData): String = SAD_FINISH_TRANSITION

  override val stepId: String
    get() = "promote.student.pack"
}