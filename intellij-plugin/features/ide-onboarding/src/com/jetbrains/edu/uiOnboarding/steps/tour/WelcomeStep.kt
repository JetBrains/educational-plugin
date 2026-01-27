// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding.steps.tour

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.ui.GotItComponentBuilder
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimation
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.EYE_SHIFT
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.SMALL_SHIFT
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationStep
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingBundle
import com.jetbrains.edu.uiOnboarding.steps.GotItBalloonStepData
import com.jetbrains.edu.uiOnboarding.steps.locateZhabaInProjectToolWindow
import java.awt.Point

class WelcomeStep : EduUiOnboardingStep(STEP_KEY) {

  override fun buildAnimation(data: EduUiOnboardingAnimationData, point: RelativePoint): EduUiOnboardingAnimation =
    object : EduUiOnboardingAnimation {
      override val steps: List<EduUiOnboardingAnimationStep> = listOfNotNull(
        EduUiOnboardingAnimationStep(data.lookRight, point, point, 2_000),
        EduUiOnboardingAnimationStep(data.lookLeft, point, point, 1_000),
      )

      override val cycle: Boolean = true
    }

  override fun performStep(
    project: Project,
    data: EduUiOnboardingAnimationData,
  ): GotItBalloonStepData? {
    val relativeZhabaPoint = locateZhabaInProjectToolWindow(project) ?: return null
    val zhabaPoint = relativeZhabaPoint.originalPoint
    val component = relativeZhabaPoint.originalComponent

    val zhabaComponent = createZhaba(project, data, relativeZhabaPoint)

    val builder = GotItComponentBuilder { EduUiOnboardingBundle.message("welcome.step.text") }
      .withHeader(EduUiOnboardingBundle.message("welcome.step.header"))

    // Position the balloon at the bottom of the project view component
    val point = Point(zhabaPoint.x + EYE_SHIFT, zhabaPoint.y - SMALL_SHIFT)
    val relativePoint = RelativePoint(component, point)

    return GotItBalloonStepData(builder, relativePoint, relativeZhabaPoint, Balloon.Position.above, zhabaComponent)
  }

  companion object {
    const val STEP_KEY = "welcome"
  }
}
