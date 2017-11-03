package com.jetbrains.edu.python.learning

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkModel
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.util.Condition
import com.intellij.ui.ComboboxWithBrowseButton
import com.jetbrains.python.sdk.PyDetectedSdk
import com.jetbrains.python.sdk.PythonSdkType

internal class IDEAPyLanguageSettings : PyLanguageSettings() {

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
    comboBox.setSetupButton(setupButton, null, model, comboBox.model.selectedItem as JdkComboBox.JdkComboBoxItem, null, false)
    return comboBoxWithBrowseButton
  }

  private fun onSdkSelected(comboBox: JdkComboBox) {
    var selectedSdk = comboBox.selectedJdk
    if (selectedSdk == null) {
      val selectedItem = comboBox.selectedItem
      if (selectedItem is JdkComboBox.SuggestedJdkItem) {
        selectedSdk = PyDetectedSdk(selectedItem.path)
      }
    }
    mySettings.sdk = selectedSdk
  }
}
