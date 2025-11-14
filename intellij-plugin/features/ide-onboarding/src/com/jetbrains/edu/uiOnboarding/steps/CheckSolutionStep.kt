// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding.steps

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.GotItComponentBuilder
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckPanel
import com.jetbrains.edu.uiOnboarding.*
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.SMALL_SHIFT
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.ZHABA_DIMENSION
import java.awt.Point

class CheckSolutionStep : EduUiOnboardingStep {

  override fun buildAnimation(data: EduUiOnboardingAnimationData, point: RelativePoint): EduUiOnboardingAnimation =
    object : EduUiOnboardingAnimation {
      override val steps: List<EduUiOnboardingAnimationStep> = listOf(
        EduUiOnboardingAnimationStep(data.lookDown, point, point, 3_000)
      )

      override val cycle: Boolean = true
    }

  override fun performStep(
    project: Project,
    data: EduUiOnboardingAnimationData
  ): EduUiOnboardingStepData? {
    val taskToolWindow = ToolWindowManager.getInstance(project)
                           .getToolWindow("Task") ?: return null

    taskToolWindow.show()

    val component = taskToolWindow.component

    val checkPanel = component.findComponentOfType(CheckPanel::class.java) ?: return null
    val checkActionsPanel = checkPanel.checkActionsPanel

    if (!checkActionsPanel.isShowing) return null

    val builder = GotItComponentBuilder { EduUiOnboardingBundle.message("check.solution.step.text") }
      .withHeader(EduUiOnboardingBundle.message("check.solution.step.header"))

    val zhabaPoint = Point(SMALL_SHIFT, -ZHABA_DIMENSION.height - SMALL_SHIFT)
    val relativeZhabaPoint = RelativePoint(checkActionsPanel, zhabaPoint)
    val zhabaComponent = createZhaba(project, data, relativeZhabaPoint)

    val point = Point(zhabaPoint.x + ZHABA_DIMENSION.width / 2, zhabaPoint.y - SMALL_SHIFT)
    val relativePoint = RelativePoint(checkActionsPanel, point)
    return EduUiOnboardingStepData(builder, relativePoint, relativeZhabaPoint, Balloon.Position.above, zhabaComponent)
  }

  override fun isAvailable(): Boolean = true

  companion object {
    const val STEP_KEY: String = "checkSolution"
  }
}

private fun <T : Any> java.awt.Component.findComponentOfType(clazz: Class<T>): T? {
  if (clazz.isInstance(this)) return clazz.cast(this)
  if (this is java.awt.Container) {
    for (i in 0 until componentCount) {
      val component = getComponent(i)
      val result = component.findComponentOfType(clazz)
      if (result != null) return result
    }
  }
  return null
}
