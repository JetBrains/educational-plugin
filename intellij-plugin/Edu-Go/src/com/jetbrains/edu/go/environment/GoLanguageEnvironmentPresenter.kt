package com.jetbrains.edu.go.environment

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.vfs.VfsUtilCore
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.go.messages.EduGoBundle
import com.jetbrains.edu.learning.newproject.ui.environment.LanguageEnvironmentPresenter
import javax.swing.Icon

class GoLanguageEnvironmentPresenter : LanguageEnvironmentPresenter<GoLanguageEnvironment> {
  override fun label(): @NlsContexts.Label String = EduGoBundle.message("sdk.label.text")

  override fun name(environment: GoLanguageEnvironment): String {
    return when (environment) {
      is GoLanguageEnvironment.Existing -> environment.sdk.version ?: EduGoBundle.message("sdk.unknown.version")
      is GoLanguageEnvironment.Install -> EduGoBundle.message("sdk.install.name")
      GoLanguageEnvironment.NoOp -> ""
    }
  }

  override fun secondaryText(environment: GoLanguageEnvironment): String? {
    return when (environment) {
      is GoLanguageEnvironment.Existing -> VfsUtilCore.urlToPath(environment.sdk.homeUrl)
      is GoLanguageEnvironment.Install -> EduGoBundle.message("sdk.install.description", environment.version)
      GoLanguageEnvironment.NoOp -> null
    }
  }

  override fun icon(environment: GoLanguageEnvironment): Icon? {
    return when (environment) {
      is GoLanguageEnvironment.Existing -> EducationalCoreIcons.Language.Go
      is GoLanguageEnvironment.Install -> AllIcons.Actions.Download
      GoLanguageEnvironment.NoOp -> null
    }
  }
}
