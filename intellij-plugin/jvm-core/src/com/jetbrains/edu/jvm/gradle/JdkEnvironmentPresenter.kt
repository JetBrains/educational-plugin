package com.jetbrains.edu.jvm.gradle

import com.intellij.icons.AllIcons
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.SdkType
import com.intellij.openapi.util.NlsContexts
import com.jetbrains.edu.jvm.environment.JdkBasedLanguageEnvironment
import com.jetbrains.edu.jvm.environment.JdkLanguageEnvironment
import com.jetbrains.edu.jvm.environment.JdkLanguageEnvironmentExisting
import com.jetbrains.edu.jvm.environment.JdkLanguageEnvironmentInstall
import com.jetbrains.edu.jvm.messages.EduJVMBundle
import com.jetbrains.edu.learning.newproject.ui.environment.LanguageEnvironmentPresenter
import javax.swing.Icon

class JdkEnvironmentPresenter : LanguageEnvironmentPresenter<JdkLanguageEnvironment> {
  override fun label(): @NlsContexts.Label String = EduJVMBundle.message("jdk.environment.label")

  override fun name(environment: JdkLanguageEnvironment): String = environment.toBase().itemName

  override fun secondaryText(environment: JdkLanguageEnvironment): String {
    val baseEnvironment = environment.toBase()

    return if (baseEnvironment is JdkLanguageEnvironmentInstall) {
      EduJVMBundle.message("jdk.environment.download", baseEnvironment.downloadSize, baseEnvironment.homePath)
    }
    else {
      baseEnvironment.homePath
    }
  }

  override fun icon(environment: JdkLanguageEnvironment): Icon? {
    return when(val base = environment.toBase()) {
      is JdkLanguageEnvironmentInstall -> AllIcons.Actions.Download
      is JdkLanguageEnvironmentExisting -> (base.existingJdk.sdkType as? SdkType)?.icon ?: JavaSdk.getInstance().icon
      else -> JavaSdk.getInstance().icon
    }
  }

  private fun JdkLanguageEnvironment.toBase(): JdkBasedLanguageEnvironment {
    when (this) {
      is JdkBasedLanguageEnvironment -> return this
      else -> error("Only base environments could be presented")
    }
  }
}
