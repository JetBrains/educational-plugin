package com.jetbrains.edu.learning

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.ui.ComboboxWithBrowseButton
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.python.configuration.PyConfigurableInterpreterList
import com.jetbrains.python.newProject.steps.PythonSdkChooserCombo

internal class PyCharmPyDirectoryProjectGenerator(isLocal: Boolean) : PyDirectoryProjectGenerator(isLocal) {
  override fun addSdk(project: Project, sdk: Sdk) {
    val model = PyConfigurableInterpreterList.getInstance(project).model
    model.addSdk(sdk)
    try {
      model.apply()
    } catch (e: ConfigurationException) {
      LOG.error("Error adding detected python interpreter " + e.message, e)
    }
  }

  override fun getAllSdks(project: Project): MutableList<Sdk> =
          PyConfigurableInterpreterList.getInstance(project).allPythonSdks

  override fun getInterpreterComboBox(project: Project, registeredSdks: List<Sdk>, fakeSdk: Sdk?): ComboboxWithBrowseButton {
    val sdks = if (fakeSdk != null) ContainerUtil.prepend(registeredSdks, fakeSdk) else registeredSdks
    val sdkChooser = PythonSdkChooserCombo(project, sdks) { true }
    sdkChooser.addChangedListener { onSdkSelected(sdkChooser.comboBox.selectedItem as? Sdk) }
    if (fakeSdk != null) {
      sdkChooser.comboBox.selectedItem = fakeSdk
    }
    return sdkChooser
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(PyCharmPyDirectoryProjectGenerator::class.java)
  }
}
