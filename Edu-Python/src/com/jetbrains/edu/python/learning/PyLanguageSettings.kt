package com.jetbrains.edu.python.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkModel
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.ComboboxWithBrowseButton
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduPluginConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.python.configuration.VirtualEnvProjectFilter
import com.jetbrains.edu.python.learning.PyDirectoryProjectGenerator.getBaseSdk
import com.jetbrains.python.newProject.PyNewProjectSettings
import com.jetbrains.python.sdk.PyDetectedSdk
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor
import icons.PythonIcons
import java.awt.BorderLayout
import javax.swing.JComponent
internal open class PyLanguageSettings : EduPluginConfigurator.LanguageSettings<PyNewProjectSettings> {

  protected val mySettings: PyNewProjectSettings = PyNewProjectSettings()

  override fun getLanguageSettingsComponent(course: Course): LabeledComponent<JComponent> {
    val project = ProjectManager.getInstance().defaultProject
    val sdks = getAllSdks(project)
    VirtualEnvProjectFilter.removeAllAssociated(sdks)
    // by default we create new virtual env in project, we need to add this non-existing sdk to sdk list
    val fakeSdk = createFakeSdk(course)

    val combo = getInterpreterComboBox(project, sdks, fakeSdk)
    if (SystemInfo.isMac && !UIUtil.isUnderDarcula()) {
      combo.putClientProperty("JButton.buttonType", null)
    }
    combo.setButtonIcon(PythonIcons.Python.InterpreterGear)
    return LabeledComponent.create<JComponent>(combo, "Interpreter", BorderLayout.WEST)
  }

  override fun getSettings(): PyNewProjectSettings = mySettings

  open fun getAllSdks(project: Project): List<Sdk> =
          ProjectJdkTable.getInstance().getSdksOfType(PythonSdkType.getInstance())

  // We ignore `registeredSdks` here because `ProjectSdksModel` collects sdk list itself.
  // So we should just add fake sdk into it
  open fun getInterpreterComboBox(project: Project, registeredSdks: List<Sdk>, fakeSdk: Sdk?): ComboboxWithBrowseButton {
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

  companion object {

    private fun createFakeSdk(course: Course): ProjectJdkImpl? {
      val fakeSdkPath = getBaseSdk(course) ?: return null
      val flavor = PythonSdkFlavor.getApplicableFlavors(false)[0]
      val prefix = flavor.name + " "
      val versionString = flavor.getVersionString(fakeSdkPath)
      if (versionString == null || !versionString.contains(prefix)) {
        return null
      }
      val name = "new virtual env " + versionString.substring(prefix.length)
      return ProjectJdkImpl(name, FakePythonSdkType)
    }
  }
}
