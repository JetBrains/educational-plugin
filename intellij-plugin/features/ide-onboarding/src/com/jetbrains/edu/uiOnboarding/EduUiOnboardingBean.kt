package com.jetbrains.edu.uiOnboarding

import com.intellij.openapi.extensions.ExtensionPointName

// copy-pasted from mono-repo
class EduUiOnboardingBean {

  companion object {
    private val EP_NAME: ExtensionPointName<EduUiOnboardingBean> = ExtensionPointName("com.intellij.ide.eduUiOnboarding")

    fun getInstance(): EduUiOnboardingBean {
      return EP_NAME.findFirstSafe { true } ?: error("NewUiOnboarding bean must be defined")
    }
  }
}