package com.jetbrains.edu.python.learning

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.ui.ComboboxWithBrowseButton
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.python.configuration.PyConfigurableInterpreterList
import com.jetbrains.python.configuration.VirtualEnvProjectFilter
import com.jetbrains.python.newProject.steps.PythonSdkChooserCombo

internal class PyCharmPyLanguageSettings : PyLanguageSettings() {

  override fun getInterpreterComboBox(fakeSdk: Sdk?): ComboboxWithBrowseButton {
    val registeredSdks = PyConfigurableInterpreterList.getInstance(null).allPythonSdks
    VirtualEnvProjectFilter.removeAllAssociated(registeredSdks)
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
