package com.jetbrains.edu.python.learning.pycharm

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.ui.ComboboxWithBrowseButton
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.edu.python.learning.newproject.PyLanguageSettings
import com.jetbrains.python.configuration.PyConfigurableInterpreterList
import com.jetbrains.python.newProject.steps.PythonSdkChooserCombo
import com.jetbrains.python.sdk.PythonSdkAdditionalData
import com.jetbrains.python.sdk.PythonSdkType

internal class PyLanguageSettings : PyLanguageSettings() {

  override fun getInterpreterComboBox(fakeSdk: Sdk?): ComboboxWithBrowseButton {
    val registeredSdks = PyConfigurableInterpreterList.getInstance(null).allPythonSdks
    registeredSdks.removeIf { it ->
      if (it != null && PythonSdkType.isVirtualEnv(it)) {
        val data = it.sdkAdditionalData as PythonSdkAdditionalData?
        if (data != null) {
          val path = data.associatedProjectPath
          if (path != null) {
            return@removeIf true
          }
        }
      }
      false
    }

    val sdks = if (fakeSdk != null) ContainerUtil.prepend(registeredSdks, fakeSdk) else registeredSdks
    val sdkChooser = PythonSdkChooserCombo(null, sdks, null) { true }
    sdkChooser.addChangedListener {
      mySettings.sdk = sdkChooser.comboBox.selectedItem as? Sdk
    }
    if (fakeSdk != null) {
      sdkChooser.comboBox.selectedItem = fakeSdk
    }
    return sdkChooser
  }
}
