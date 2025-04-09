package com.jetbrains.edu.uiOnboarding

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.util.KeyedLazyInstanceEP

// copy-pasted from mono-repo
interface EduUiOnboardingStep {
  suspend fun performStep(project: Project, disposable: CheckedDisposable): EduUiOnboardingStepData?

  fun isAvailable(): Boolean = true

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