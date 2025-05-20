// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding.steps

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.ui.GotItComponentBuilder
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.*
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.EYE_SHIFT
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.ZHABA_DIMENSION
import java.awt.Point

class CodeEditorStep : EduUiOnboardingStep {

  override fun buildAnimation(data: EduUiOnboardingAnimationData, point: RelativePoint): EduUiOnboardingAnimation =
    object : EduUiOnboardingAnimation {
      override val steps: List<EduUiOnboardingAnimationStep> = listOf(
        EduUiOnboardingAnimationStep(data.pointingLeft1, point, point, 2_000, Point(110, 0)),
        EduUiOnboardingAnimationStep(data.pointingLeft2, point, point, 1_000, Point(110, 0)),
      )

      override val cycle: Boolean = true
    }

  override suspend fun performStep(
    project: Project,
    data: EduUiOnboardingAnimationData,
    disposable: CheckedDisposable
  ): EduUiOnboardingStepData? {
    val taskToolWindow = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
                           .getToolWindow("Task") ?: return null

    taskToolWindow.show()

    val component = taskToolWindow.component

    val builder = GotItComponentBuilder { EduUiOnboardingBundle.message("code.editor.step.text") }
      .withHeader(EduUiOnboardingBundle.message("code.editor.step.header"))

    val zhabaPoint = Point(4, component.height / 2 - ZHABA_DIMENSION.height / 2)
    val relativeZhabaPoint = RelativePoint(component, zhabaPoint)

    val tooltipPoint = Point(zhabaPoint.x + ZHABA_DIMENSION.width - EYE_SHIFT, zhabaPoint.y + ZHABA_DIMENSION.height + 4)
    val relativePoint = RelativePoint(component, tooltipPoint)

    val zhabaComponent = createZhaba(project, data, relativeZhabaPoint, disposable)
    return EduUiOnboardingStepData(builder, relativePoint, relativeZhabaPoint, Balloon.Position.below, zhabaComponent)
  }

  override fun isAvailable(): Boolean = true

  companion object {
    const val STEP_KEY: String = "codeEditor"
  }
}