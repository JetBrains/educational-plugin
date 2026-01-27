// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
import java.awt.Point

class TaskDescriptionStep : EduUiOnboardingStep(STEP_KEY) {
  override fun buildAnimation(data: EduUiOnboardingAnimationData, point: RelativePoint): EduUiOnboardingAnimation =
    object : EduUiOnboardingAnimation {
      override val steps: List<EduUiOnboardingAnimationStep> = listOf(
        EduUiOnboardingAnimationStep(data.pointingRight1, point, point, 2_000),
        EduUiOnboardingAnimationStep(data.pointingRight2, point, point, 1_000),
      )

      override val cycle: Boolean = true
    }

  override fun performStep(
    project: Project,
    data: EduUiOnboardingAnimationData
  ): GotItBalloonStepData? {
    val taskToolWindow = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
                           .getToolWindow("Task") ?: return null

    taskToolWindow.show()

    val component = taskToolWindow.component
    if (!component.isShowing) return null

    val builder = GotItComponentBuilder { EduUiOnboardingBundle.message("task.description.step.text") }
      .withHeader(EduUiOnboardingBundle.message("task.description.step.header"))

    val zhabaPoint = Point(SMALL_SHIFT, component.height / 2 - ZHABA_DIMENSION.height / 2)
    val relativeZhabaPoint = RelativePoint(component, zhabaPoint)

    val tooltipPoint = Point(zhabaPoint.x + EYE_SHIFT, zhabaPoint.y - SMALL_SHIFT)
    val relativePoint = RelativePoint(component, tooltipPoint)

    val zhabaComponent = createZhaba(project, data, relativeZhabaPoint)
    return GotItBalloonStepData(builder, relativePoint, relativeZhabaPoint, Balloon.Position.above, zhabaComponent)
  }

  companion object {
    const val STEP_KEY = "taskDescription"
  }
}