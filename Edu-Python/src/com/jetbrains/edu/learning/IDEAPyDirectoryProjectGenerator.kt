package com.jetbrains.edu.learning

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkModel
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ComboboxWithBrowseButton
import com.jetbrains.python.sdk.PyDetectedSdk
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.PythonSdkUpdater

internal class IDEAPyDirectoryProjectGenerator(isLocal: Boolean) : PyDirectoryProjectGenerator(isLocal) {

  override fun getAllSdks(): List<Sdk> =
          ProjectJdkTable.getInstance().getSdksOfType(PythonSdkType.getInstance())

  override fun getInterpreterComboBox(fakeSdk: Sdk?): ComboboxWithBrowseButton {
    val project = ProjectManager.getInstance().defaultProject
    val model = ProjectSdksModel()
    model.reset(project)
    if (fakeSdk != null) {
      model.addSdk(fakeSdk)
    }

    model.addListener(object : SdkModel.Listener {
      override fun sdkAdded(sdk: Sdk) = SdkConfigurationUtil.addSdk(sdk)
      override fun beforeSdkRemove(sdk: Sdk) {}
      override fun sdkChanged(sdk: Sdk, previousName: String) {}
      override fun sdkHomeSelected(sdk: Sdk, newSdkHome: String) {}
    })

    val sdkTypeIdFilter = Condition<SdkTypeId> { it == PythonSdkType.getInstance() || it == FakePythonSdkType }
    val sdkFilter = JdkComboBox.getSdkFilter(sdkTypeIdFilter)
    val comboBox = JdkComboBox(model, sdkTypeIdFilter, sdkFilter, sdkTypeIdFilter, true)
    comboBox.addActionListener { onSdkSelected(comboBox) }

    if (fakeSdk != null) {
      comboBox.selectedJdk = fakeSdk
    }

    val comboBoxWithBrowseButton = ComboboxWithBrowseButton(comboBox)
    val setupButton = comboBoxWithBrowseButton.button
    comboBox.setSetupButton(setupButton, null, model, comboBox.getModel().getSelectedItem() as JdkComboBox.JdkComboBoxItem, null, false)
    return comboBoxWithBrowseButton
  }

  override fun updateSdkIfNeeded(project: Project, sdk: Sdk?): Sdk? {
    return if (sdk is PyDetectedSdk) {
      val name = sdk.name
      val sdkHome = WriteAction.compute<VirtualFile, RuntimeException> { LocalFileSystem.getInstance().refreshAndFindFileByPath(name) }
      val newSdk = SdkConfigurationUtil.createAndAddSDK(sdkHome.path, PythonSdkType.getInstance())
      if (newSdk != null) {
        PythonSdkUpdater.updateOrShowError(newSdk, null, project, null)
        SdkConfigurationUtil.addSdk(newSdk)
      }
      newSdk
    } else sdk
  }

  private fun onSdkSelected(comboBox: JdkComboBox) {
    var selectedSdk = comboBox.selectedJdk
    if (selectedSdk == null) {
      val selectedItem = comboBox.selectedItem
      if (selectedItem is JdkComboBox.SuggestedJdkItem) {
        selectedSdk = PyDetectedSdk(selectedItem.path)
      }
    }
    onSdkSelected(selectedSdk)
  }
}
