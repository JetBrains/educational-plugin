package com.jetbrains.edu.python.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.ui.ComboboxWithBrowseButton
import com.intellij.util.PlatformUtils
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.edu.python.learning.newproject.PySdkSettingsHelper
import com.jetbrains.python.configuration.PyConfigurableInterpreterList
import com.jetbrains.python.newProject.steps.PythonSdkChooserCombo
import com.jetbrains.python.sdk.PythonSdkAdditionalData
import com.jetbrains.python.sdk.PythonSdkUtil

class PyPyCharmSdkSettingsHelper : PySdkSettingsHelper {
  override fun isAvailable(): Boolean = PlatformUtils.isPyCharm() || PlatformUtils.isCLion()

  override fun getInterpreterComboBox(fakeSdk: Sdk?, onSdkSelected: (Sdk?) -> Unit): ComboboxWithBrowseButton {
    val registeredSdks = PyConfigurableInterpreterList.getInstance(null).allPythonSdks
    registeredSdks.removeIf {
      if (it != null && PythonSdkUtil.isVirtualEnv(it)) {
        val data = it.sdkAdditionalData as PythonSdkAdditionalData?
        if (data != null) {
          val path = data.associatedModulePath
          if (path != null) {
            return@removeIf true
          }
        }
      }
      false
    }

    val sdks = if (fakeSdk != null) ContainerUtil.prepend(registeredSdks, fakeSdk) else registeredSdks
    val sdkChooser = PythonSdkChooserCombo(null, null, sdks, null) { true }
    sdkChooser.addChangedListener {
      val sdk = sdkChooser.comboBox.selectedItem as? Sdk
      onSdkSelected(sdk)
    }
    if (fakeSdk != null) {
      sdkChooser.comboBox.selectedItem = fakeSdk
    }
    return sdkChooser
  }

  override fun getAllSdks(): List<Sdk> = PyConfigurableInterpreterList.getInstance(null).allPythonSdks

  override fun updateSdkIfNeeded(project: Project, sdk: Sdk?): Sdk? = sdk
}
