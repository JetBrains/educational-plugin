package com.jetbrains.edu.python.learning.pycharm

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.ui.ComboboxWithBrowseButton
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.edu.python.learning.newproject.PyLanguageSettings
import com.jetbrains.python.configuration.PyConfigurableInterpreterList
import com.jetbrains.python.newProject.steps.PythonSdkChooserCombo

internal class PyCharmPyLanguageSettings : PyLanguageSettings() {

  override fun getAllSdks(project: Project): MutableList<Sdk> =
          PyConfigurableInterpreterList.getInstance(project).allPythonSdks

  override fun getInterpreterComboBox(project: Project, registeredSdks: List<Sdk>, fakeSdk: Sdk?): ComboboxWithBrowseButton {
    val sdks = if (fakeSdk != null) ContainerUtil.prepend(registeredSdks, fakeSdk) else registeredSdks
    val sdkChooser = PythonSdkChooserCombo(project, sdks) { true }
    sdkChooser.addChangedListener {
      mySettings.sdk = sdkChooser.comboBox.selectedItem as? Sdk
    }
    if (fakeSdk != null) {
      sdkChooser.comboBox.selectedItem = fakeSdk
    }
    return sdkChooser
  }
}
