package com.jetbrains.edu.learning

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.ui.ComboboxWithBrowseButton
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.python.configuration.PyConfigurableInterpreterList
import com.jetbrains.python.configuration.VirtualEnvProjectFilter
import com.jetbrains.python.newProject.steps.PythonSdkChooserCombo

internal class PyCharmPyDirectoryProjectGenerator(course: Course, isLocal: Boolean) : PyDirectoryProjectGenerator(course, isLocal) {

  override fun getAllSdks(): List<Sdk> {
    // New python API passes default project into `ServiceManager.getService` if project argument is null
    // but old API passes project argument into `ServiceManager.getService` directly
    // and doesn't support null argument
    // so if we use old API we should pass default project manually
    val project = if (myHasOldPythonApi) ProjectManager.getInstance().defaultProject else null
    return PyConfigurableInterpreterList.getInstance(project).allPythonSdks
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
