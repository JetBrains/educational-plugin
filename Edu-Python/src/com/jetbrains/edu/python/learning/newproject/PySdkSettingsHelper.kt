package com.jetbrains.edu.python.learning.newproject

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import javax.swing.JComponent

interface PySdkSettingsHelper {
  fun isAvailable(): Boolean
  fun getInterpreterComboBox(fakeSdk: Sdk?, onSdkSelected: (Sdk?) -> Unit): JComponent
  fun updateSdkIfNeeded(project: Project, sdk: Sdk?): Sdk?
  fun getAllSdks(): List<Sdk>

  companion object {
    @JvmStatic
    val EP_NAME: ExtensionPointName<PySdkSettingsHelper> = ExtensionPointName.create("Educational.pySdkSettingsHelper")

    @JvmStatic
    fun firstAvailable(): PySdkSettingsHelper = EP_NAME.extensionList.first(PySdkSettingsHelper::isAvailable)
  }
}
