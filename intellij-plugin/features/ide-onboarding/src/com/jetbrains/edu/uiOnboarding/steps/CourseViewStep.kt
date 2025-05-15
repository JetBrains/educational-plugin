// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding.steps

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.ui.GotItComponentBuilder
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.*
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.ZHABA_DIMENSION
import java.awt.Point

class CourseViewStep : EduUiOnboardingStep {
  override fun buildAnimation(data: EduUiOnboardingAnimationData, point: RelativePoint): EduUiOnboardingAnimation =
    object : EduUiOnboardingAnimation {
      override val steps: List<EduUiOnboardingAnimationStep> = listOf(
        EduUiOnboardingAnimationStep(data.lookUp, point, point, 10_000),
      )

      override val cycle: Boolean = true
    }

  override suspend fun performStep(
    project: Project,
    data: EduUiOnboardingAnimationData,
    disposable: CheckedDisposable
  ): EduUiOnboardingStepData? {
    val projectViewToolWindow = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
                                  .getToolWindow("Project") ?: return null

    projectViewToolWindow.show()

    val component = projectViewToolWindow.component

    val builder = GotItComponentBuilder { EduUiOnboardingBundle.message("course.view.step.text") }
      .withHeader(EduUiOnboardingBundle.message("course.view.step.header"))

    val zhabaPoint = Point(
      (component.width - ZHABA_DIMENSION.width) / 2,
      component.height - ZHABA_DIMENSION.height
    )

    val relativeZhabaPoint = RelativePoint(component, zhabaPoint)
    val zhabaComponent = createZhaba(project, data, relativeZhabaPoint, disposable)

    // Position the balloon a bit to the right from the middle of the project view
    val point = Point(zhabaPoint.x + ZHABA_DIMENSION.width / 2, zhabaPoint.y - 4)
    val relativePoint = RelativePoint(component, point)

    return EduUiOnboardingStepData(builder, relativePoint, relativeZhabaPoint, Balloon.Position.above, zhabaComponent)
  }

  override fun isAvailable(): Boolean = true
}
