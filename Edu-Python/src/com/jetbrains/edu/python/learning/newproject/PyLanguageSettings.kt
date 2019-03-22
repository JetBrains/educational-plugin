package com.jetbrains.edu.python.learning.newproject

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.ProjectManager
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
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.ErrorMessage
import com.jetbrains.python.newProject.PyNewProjectSettings
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.sdk.PyDetectedSdk
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor
import java.awt.BorderLayout
import javax.swing.JComponent

internal open class PyLanguageSettings : LanguageSettings<PyNewProjectSettings>() {

  protected val mySettings: PyNewProjectSettings = PyNewProjectSettings()

  override fun getLanguageSettingsComponents(course: Course): List<LabeledComponent<JComponent>> {
    // by default we create new virtual env in project, we need to add this non-existing sdk to sdk list
    val fakeSdk = createFakeSdk(course)

    val combo = getInterpreterComboBox(fakeSdk)
    if (SystemInfo.isMac && !UIUtil.isUnderDarcula()) {
      combo.putClientProperty("JButton.buttonType", null)
    }
    combo.setButtonIcon(AllIcons.General.GearPlain)
    return listOf<LabeledComponent<JComponent>>(LabeledComponent.create(combo, "Interpreter", BorderLayout.WEST))
  }

  override fun getSettings(): PyNewProjectSettings = mySettings

  override fun getLanguageVersions(): List<String> {
    val pythonVersions = mutableListOf(ALL_VERSIONS, PYTHON_3, PYTHON_2)
    pythonVersions.addAll(LanguageLevel.values().map { it.toString() }.reversed())
    return pythonVersions
  }

  override fun validate(course: Course?): ErrorMessage? {
    course ?: return null
    val sdk = mySettings.sdk ?: return ErrorMessage("Please specify Python interpreter")
    return (isSdkApplicable(course, sdk.languageLevel) as? Err)?.error?.let { ErrorMessage(it) }
  }

  private val Sdk.languageLevel: LanguageLevel
    get() {
      return if (sdkType === PyFakeSdkType) {
        val pythonVersion = versionString
        if (pythonVersion == null) LanguageLevel.getDefault() else LanguageLevel.fromPythonVersion(pythonVersion)
      }
      else {
        PythonSdkType.getLanguageLevelForSdk(this)
      }
    }

  protected open fun getInterpreterComboBox(fakeSdk: Sdk?): ComboboxWithBrowseButton {
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

    val sdkTypeIdFilter = Condition<SdkTypeId> { it == PythonSdkType.getInstance() || it == PyFakeSdkType }
    val sdkFilter = JdkComboBox.getSdkFilter(sdkTypeIdFilter)
    val comboBox = JdkComboBox(model, sdkTypeIdFilter, sdkFilter, sdkTypeIdFilter, true)
    comboBox.addActionListener { onSdkSelected(comboBox) }

    if (fakeSdk != null) {
      comboBox.selectedJdk = fakeSdk
    }

    val comboBoxWithBrowseButton = ComboboxWithBrowseButton(comboBox)
    val setupButton = comboBoxWithBrowseButton.button
    comboBox.setSetupButton(setupButton, null, model, comboBox.model.selectedItem as JdkComboBox.JdkComboBoxItem?, null, false)
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
    notifyListeners()
  }

  companion object {

    private val OK = Ok(Unit)

    private fun isSdkApplicable(course: Course, sdkLanguageLevel: LanguageLevel): Result<Unit, String> {
      val courseLanguageVersion = course.languageVersion
      val isPython2Sdk = sdkLanguageLevel.isPython2

      return when (courseLanguageVersion) {
        null, ALL_VERSIONS -> OK
        PYTHON_2 -> if (isPython2Sdk) OK else NoApplicablePythonError(2)
        PYTHON_3 -> if (!isPython2Sdk) OK else NoApplicablePythonError(3)
        else -> {
          val courseLanguageLevel = LanguageLevel.fromPythonVersion(courseLanguageVersion)
          when {
            courseLanguageLevel.isPython2 != isPython2Sdk -> SpecificPythonRequiredError(courseLanguageVersion)
            sdkLanguageLevel.isAtLeast(courseLanguageLevel) -> OK
            else -> SpecificPythonRequiredError(courseLanguageVersion)
          }
        }
      }
    }

    @JvmStatic
    fun getBaseSdk(course: Course): String? {
      val flavor = PythonSdkFlavor.getApplicableFlavors(false)[0]

      // BACKCOMPAT: 2018.2
      // Replace `flavor.suggestHomePaths()` with `flavor.suggestHomePaths(null)`
      @Suppress("DEPRECATION")
      val sdkPaths = ArrayList<String>(flavor.suggestHomePaths())

      if (sdkPaths.isEmpty()) {
        return null
      }
      return sdkPaths.filter {
        isSdkApplicable(course, flavor.getLanguageLevel(it)) == OK && flavor.getVersionString(it) != null
      }.maxBy {
        flavor.getVersionString(it)!!
      }
    }

    private class NoApplicablePythonError(requiredVersion: Int) : Err<String>("Required Python $requiredVersion")
    private class SpecificPythonRequiredError(requiredVersion: String) : Err<String>("Required at least Python $requiredVersion")

    private fun createFakeSdk(course: Course): ProjectJdkImpl? {
      val fakeSdkPath = getBaseSdk(course) ?: return null
      val flavor = PythonSdkFlavor.getApplicableFlavors(false)[0]
      val prefix = flavor.name + " "
      val versionString = flavor.getVersionString(fakeSdkPath)
      if (versionString == null || !versionString.contains(prefix)) {
        return null
      }
      val pythonVersion = versionString.substring(prefix.length)
      val name = "new virtual env $pythonVersion"
      return ProjectJdkImpl(name, PyFakeSdkType, "", pythonVersion)
    }

    private const val ALL_VERSIONS = "All versions"

    const val PYTHON_3 = "3.x"
    const val PYTHON_2 = "2.x"
  }
}
