package com.jetbrains.edu.uiOnboarding.steps

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.BalloonBuilder
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimation
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.EYE_SHIFT
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.SMALL_SHIFT
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationStep
import com.jetbrains.edu.uiOnboarding.ZhabaComponent
import com.jetbrains.edu.uiOnboarding.stepsGraph.ActionGroupZhabaData
import com.jetbrains.edu.uiOnboarding.stepsGraph.GraphData
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.FINISH_TRANSITION
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import java.awt.Point
import javax.swing.JComponent
import javax.swing.JLabel


class ActionGroupZhabaStep(override val stepId: String, private val actionGroup: ActionGroup) : ZhabaStep<ActionGroupZhabaData, GraphData.EMPTY> {

  private fun buildAnimation(data: EduUiOnboardingAnimationData, point: RelativePoint): EduUiOnboardingAnimation =
    object : EduUiOnboardingAnimation {
      override val steps: List<EduUiOnboardingAnimationStep> = listOfNotNull(
        EduUiOnboardingAnimationStep(data.lookRight, point, point, 3_000),
      )

      override val cycle: Boolean = true
    }

  override fun performStep(
    project: Project,
    data: EduUiOnboardingAnimationData
  ): ActionGroupZhabaData? {
    val relativeZhabaPoint = locateZhabaInProjectToolWindow(project) ?: return null
    val zhabaPoint = relativeZhabaPoint.originalPoint
    val component = relativeZhabaPoint.originalComponent

    val zhabaComponent = ZhabaComponent(project)
    zhabaComponent.animation = buildAnimation(data, relativeZhabaPoint)

    // Position the balloon at the bottom of the project view component
    val tooltipPoint = Point(zhabaPoint.x + EYE_SHIFT, zhabaPoint.y - SMALL_SHIFT)
    val tooltipRelativePoint = RelativePoint(component, tooltipPoint)

    val builder = JBPopupFactory.getInstance().createBalloonBuilder(JLabel("hello"))

    return ActionGroupZhabaData(builder, tooltipRelativePoint, Balloon.Position.above, relativeZhabaPoint, zhabaComponent)
  }

  override suspend fun executeStep(
    stepData: ActionGroupZhabaData,
    graphData: GraphData.EMPTY,
    cs: CoroutineScope,
    disposable: Disposable
  ): String {
    val builder = stepData.builder
    builder.setCloseButtonEnabled(true)
    builder.setDisposable(disposable)
    builder.setHideOnAction(true)
    builder.setTitle("This is a title")

    val showInCenter = stepData.position == null
    val balloon = builder.createBalloon()

    if (showInCenter) {
      balloon.showInCenterOf(stepData.tooltipPoint.originalComponent as JComponent)
    }
    else {
      balloon.show(stepData.tooltipPoint, stepData.position)
    }

    delay(5000)
    return FINISH_TRANSITION
  }
}