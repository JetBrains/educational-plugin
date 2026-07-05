package com.jetbrains.edu.rust.environment

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.NlsContexts
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.newproject.ui.environment.LanguageEnvironmentPresenter
import com.jetbrains.edu.rust.messages.EduRustBundle
import javax.swing.Icon

class RsLanguageEnvironmentPresenter : LanguageEnvironmentPresenter<RsLanguageEnvironment> {
  override fun label(): @NlsContexts.Label String {
    return EduRustBundle.message("toolchain.label.text")
  }

  override fun name(environment: RsLanguageEnvironment): String {
    return when (environment) {
      is RsLanguageEnvironment.Existing -> environment.toolchain.presentableLocation
      RsLanguageEnvironment.Install -> EduRustBundle.message("toolchain.install")
      RsLanguageEnvironment.NoOp -> ""
    }
  }

  override fun secondaryText(environment: RsLanguageEnvironment): String? {
    return null
  }

  override fun icon(environment: RsLanguageEnvironment): Icon? {
    return when (environment) {
      RsLanguageEnvironment.Install -> AllIcons.Actions.Download
      is RsLanguageEnvironment.Existing -> EducationalCoreIcons.Language.Rust
      RsLanguageEnvironment.NoOp -> null
    }
  }
}