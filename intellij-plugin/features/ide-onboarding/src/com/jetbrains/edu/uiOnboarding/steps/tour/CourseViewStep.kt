// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding.steps.tour

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.ui.GotItComponentBuilder
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.*
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.SMALL_SHIFT
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.ZHABA_DIMENSION
import com.jetbrains.edu.uiOnboarding.steps.GotItBalloonStepData
import com.jetbrains.edu.uiOnboarding.steps.locateZhabaInProjectToolWindow
import java.awt.Point

class CourseViewStep : EduUiOnboardingStep(stepId) {
  override fun buildAnimation(data: EduUiOnboardingAnimationData, point: RelativePoint): EduUiOnboardingAnimation =
    object : EduUiOnboardingAnimation {
      override val steps: List<EduUiOnboardingAnimationStep> = listOf(
        EduUiOnboardingAnimationStep(data.lookUp, point, point, 3_000),
      )

      override val cycle: Boolean = true
    }

  override fun performStep(
    project: Project,
    data: EduUiOnboardingAnimationData
  ): GotItBalloonStepData? {
    val relativeZhabaPoint = locateZhabaInProjectToolWindow(project) ?: return null
    val zhabaPoint = relativeZhabaPoint.originalPoint
    val component = relativeZhabaPoint.originalComponent

    val builder = GotItComponentBuilder { EduUiOnboardingBundle.message("course.view.step.text") }
      .withHeader(EduUiOnboardingBundle.message("course.view.step.header"))

    val zhabaComponent = createZhaba(project, data, relativeZhabaPoint)

    // Position the balloon a bit to the right from the middle of the project view
    val point = Point(zhabaPoint.x + ZHABA_DIMENSION.width / 2, zhabaPoint.y - SMALL_SHIFT)
    val relativePoint = RelativePoint(component, point)

    return GotItBalloonStepData(builder, relativePoint, relativeZhabaPoint, Balloon.Position.above, zhabaComponent)
  }

  companion object : EduUiOnboardingStepFactory {
    override val stepId: String = "courseView"
    override fun create(): EduUiOnboardingStep = CourseViewStep()
  }
}
