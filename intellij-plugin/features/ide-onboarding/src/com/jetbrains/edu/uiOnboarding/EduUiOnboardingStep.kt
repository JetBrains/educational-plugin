package com.jetbrains.edu.uiOnboarding

import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.KeyedLazyInstanceEP

// copy-pasted from mono-repo
interface EduUiOnboardingStep {
  suspend fun performStep(project: Project, data: EduUiOnboardingAnimationData, disposable: CheckedDisposable): EduUiOnboardingStepData?

  fun isAvailable(): Boolean = true

  fun buildAnimation(data: EduUiOnboardingAnimationData, point: RelativePoint): EduUiOnboardingAnimation

  fun createZhaba(project: Project, data: EduUiOnboardingAnimationData, point: RelativePoint, parentDisposable: Disposable): ZhabaComponent {
    val zhabaComponent = ZhabaComponent(project)
    Disposer.register(parentDisposable, zhabaComponent)
    zhabaComponent.animation = buildAnimation(data, point)
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