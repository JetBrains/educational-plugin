// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding.steps

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.ui.GotItComponentBuilder
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimation
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.EYE_SHIFT
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.SMALL_SHIFT
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.ZHABA_DIMENSION
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationStep
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingBundle
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingStep
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingStepData
import java.awt.Point

class WelcomeStep : EduUiOnboardingStep {

  override fun buildAnimation(data: EduUiOnboardingAnimationData, point: RelativePoint): EduUiOnboardingAnimation =
    object : EduUiOnboardingAnimation {
      override val steps: List<EduUiOnboardingAnimationStep> = listOfNotNull(
        EduUiOnboardingAnimationStep(data.lookRight, point, point, 2_000),
        EduUiOnboardingAnimationStep(data.lookLeft, point, point, 1_000),
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

    // Position of the zhaba on the project view component
    val zhabaDimension = ZHABA_DIMENSION
    val zhabaPoint = Point(
      (component.width - zhabaDimension.width) / 2,
      component.height - zhabaDimension.height
    )
    val relativeZhabaPoint = RelativePoint(component, zhabaPoint)
    val zhabaComponent = createZhaba(project, data, relativeZhabaPoint, disposable)

    // Position the balloon at the bottom of the project view component
    val point = Point(zhabaPoint.x + EYE_SHIFT, zhabaPoint.y - SMALL_SHIFT)
    val relativePoint = RelativePoint(component, point)

    val builder = GotItComponentBuilder { EduUiOnboardingBundle.message("welcome.step.text") }
      .withHeader(EduUiOnboardingBundle.message("welcome.step.header"))

    return EduUiOnboardingStepData(builder, relativePoint, relativeZhabaPoint, Balloon.Position.above, zhabaComponent)
  }

  override fun isAvailable(): Boolean = true

  companion object {
    const val STEP_KEY = "welcome"
  }
}
