package com.jetbrains.edu.python.learning.newproject

import com.intellij.icons.AllIcons
import com.jetbrains.edu.learning.newproject.ui.environment.LanguageEnvironmentPresenter
import com.jetbrains.edu.python.learning.environment.PyLanguageEnvironment
import com.jetbrains.edu.python.learning.messages.EduPythonBundle.message
import com.jetbrains.python.sdk.PySdkToInstall
import com.jetbrains.python.sdk.PythonSdkType
import javax.swing.Icon

object PyEnvironmentPresenter : LanguageEnvironmentPresenter<PyLanguageEnvironment> {
  override fun label(): String = message("install.environment.choose.python.interpreter")

  override fun name(environment: PyLanguageEnvironment): String {
    return when (environment.sdk) {
      is PySdkToInstall -> message("install.environment.install.python", environment.languageLevel)
      is PySdkToCreateVirtualEnv -> message("install.environment.new.venv", environment.languageLevel)
      else -> "Python ${environment.languageLevel}"
    }
  }

  override fun secondaryText(environment: PyLanguageEnvironment): String? {
    return environment.sdk.homePath
  }

  override fun icon(environment: PyLanguageEnvironment): Icon? {
    return when (environment.sdk) {
      is PySdkToInstall -> AllIcons.Actions.Download
      else -> PythonSdkType.getInstance().icon
    }
  }
}