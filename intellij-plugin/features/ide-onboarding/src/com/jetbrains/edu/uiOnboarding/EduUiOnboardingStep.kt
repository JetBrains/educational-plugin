package com.jetbrains.edu.uiOnboarding

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.KeyedLazyInstanceEP

interface EduUiOnboardingStep {
  fun performStep(project: Project, data: EduUiOnboardingAnimationData): GotItBalloonStepData?

  fun isAvailable(): Boolean = true

  fun buildAnimation(data: EduUiOnboardingAnimationData, point: RelativePoint): EduUiOnboardingAnimation

  fun createZhaba(project: Project, data: EduUiOnboardingAnimationData, point: RelativePoint): ZhabaComponent {
    val zhabaComponent = ZhabaComponent(project)
    zhabaComponent.animation = buildAnimation(data, point)
    zhabaComponent.trackComponent(point.originalComponent)
    return zhabaComponent
  }

  companion object {
    val EP_NAME: ExtensionPointName<KeyedLazyInstanceEP<EduUiOnboardingStep>> = ExtensionPointName.Companion.create("com.intellij.ide.eduUiOnboarding.step")

    fun getIfAvailable(stepId: String): EduUiOnboardingStep? {
      val step = EP_NAME.findFirstSafe { it.key == stepId }?.instance
      return if (step?.isAvailable() == true) {
        step
      }
      else null
    }
  }
}