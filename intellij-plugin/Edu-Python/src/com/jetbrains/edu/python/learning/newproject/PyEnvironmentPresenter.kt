package com.jetbrains.edu.python.learning.newproject

import com.intellij.icons.AllIcons
import com.jetbrains.edu.learning.newproject.ui.environment.LanguageEnvironmentPresenter
import com.jetbrains.edu.python.learning.environment.PyLanguageEnvironment
import com.jetbrains.edu.python.learning.messages.EduPythonBundle.message
import com.jetbrains.python.sdk.PythonSdkType
import javax.swing.Icon

object PyEnvironmentPresenter : LanguageEnvironmentPresenter<PyLanguageEnvironment> {
  override fun label(): String = message("install.environment.choose.python.interpreter")

  override fun name(environment: PyLanguageEnvironment): String {
    return when (environment) {
      is PyLanguageEnvironment.Install -> message("install.environment.install.python.unknown.version")
      is PyLanguageEnvironment.Existing -> environment.title
    }
  }

  override fun secondaryText(environment: PyLanguageEnvironment): String? {
    return when (environment) {
      is PyLanguageEnvironment.Install -> null
      is PyLanguageEnvironment.Existing -> environment.secondaryText
    }
  }

  override fun icon(environment: PyLanguageEnvironment): Icon {
    return when (environment) {
      is PyLanguageEnvironment.Install -> AllIcons.Actions.Download
      is PyLanguageEnvironment.Existing -> PythonSdkType.getInstance().icon
    }
  }
}