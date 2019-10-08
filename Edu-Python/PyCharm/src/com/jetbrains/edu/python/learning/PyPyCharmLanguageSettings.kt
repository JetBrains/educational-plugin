package com.jetbrains.edu.python.learning

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.ui.ComboboxWithBrowseButton
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.edu.python.learning.newproject.PyLanguageSettingsBase
import com.jetbrains.python.configuration.PyConfigurableInterpreterList
import com.jetbrains.python.newProject.steps.PythonSdkChooserCombo
import com.jetbrains.python.sdk.PythonSdkAdditionalData

class PyPyCharmLanguageSettings : PyLanguageSettingsBase() {

  override fun getInterpreterComboBox(fakeSdk: Sdk?): ComboboxWithBrowseButton {
    val registeredSdks = PyConfigurableInterpreterList.getInstance(null).allPythonSdks
    registeredSdks.removeIf {
      if (it != null && it.isVirtualEnv) {
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
      mySettings.sdk = sdkChooser.comboBox.selectedItem as? Sdk
      notifyListeners()
    }
    if (fakeSdk != null) {
      sdkChooser.comboBox.selectedItem = fakeSdk
    }
    return sdkChooser
  }
}
