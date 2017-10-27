package com.jetbrains.edu.learning

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.ui.ComboboxWithBrowseButton
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.python.configuration.PyConfigurableInterpreterList
import com.jetbrains.python.configuration.VirtualEnvProjectFilter
import com.jetbrains.python.newProject.steps.PythonSdkChooserCombo

internal class PyCharmPyDirectoryProjectGenerator(isLocal: Boolean) : PyDirectoryProjectGenerator(isLocal) {

  override fun getAllSdks(): List<Sdk> {
    return PyConfigurableInterpreterList.getInstance(null).allPythonSdks
  }

  override fun getInterpreterComboBox(fakeSdk: Sdk?): ComboboxWithBrowseButton {
    val registeredSdks = allSdks
    VirtualEnvProjectFilter.removeAllAssociated(registeredSdks)
    val sdks = if (fakeSdk != null) ContainerUtil.prepend(registeredSdks, fakeSdk) else registeredSdks
    val sdkChooser = PythonSdkChooserCombo(null, sdks, null) { true }
    sdkChooser.addChangedListener { onSdkSelected(sdkChooser.comboBox.selectedItem as? Sdk) }
    if (fakeSdk != null) {
      sdkChooser.comboBox.selectedItem = fakeSdk
    }
    return sdkChooser
  }
}
