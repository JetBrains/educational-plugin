package com.jetbrains.edu.python.learning.newproject

import com.intellij.openapi.Disposable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.jetbrains.edu.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_PYTHON
import com.jetbrains.edu.learning.EduNames.PYTHON_2_VERSION
import com.jetbrains.edu.learning.EduNames.PYTHON_3_VERSION
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.errors.SettingsValidationResult
import com.jetbrains.edu.learning.newproject.ui.errors.ValidationMessage
import com.jetbrains.edu.python.learning.messages.EduPythonBundle
import com.jetbrains.python.newProject.PyNewProjectSettings
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.sdk.PySdkToInstall
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.add.PySdkPathChoosingComboBox
import com.jetbrains.python.sdk.add.addBaseInterpretersAsync
import com.jetbrains.python.sdk.detectSystemWideSdks
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import javax.swing.JComponent

open class PyLanguageSettings : LanguageSettings<PyNewProjectSettings>() {

  private val mySettings: PyNewProjectSettings = PyNewProjectSettings()

  override fun getLanguageSettingsComponents(
    course: Course,
    disposable: Disposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> {

    val sdkField = PySdkPathChoosingComboBox()
    sdkField.childComponent.addItemListener {
      if (it.stateChange == ItemEvent.SELECTED) {
        mySettings.sdk = sdkField.selectedSdk
        notifyListeners()
      }
    }

    addBaseInterpretersAsync(sdkField, emptyList(), null, context ?: UserDataHolderBase()) {
      val fakeSdk = createFakeSdk(course, context)
      if (fakeSdk != null) {
        sdkField.addSdkItemOnTop(fakeSdk)
        sdkField.selectedSdk = fakeSdk
      }
    }

    return listOf<LabeledComponent<JComponent>>(
      LabeledComponent.create(sdkField, EduCoreBundle.message("select.interpreter"), BorderLayout.WEST)
    )
  }

  override fun getSettings(): PyNewProjectSettings = mySettings

  override fun validate(course: Course?, courseLocation: String?): SettingsValidationResult {
    course ?: return SettingsValidationResult.OK
    val sdk = mySettings.sdk
    if (sdk == null) {
      val validationMessage = ValidationMessage(EduPythonBundle.message("error.no.python.interpreter", ""),
                                                ENVIRONMENT_CONFIGURATION_LINK_PYTHON)
      return SettingsValidationResult.Ready(validationMessage)
    }

    val sdkApplicable = isSdkApplicable(course, sdk.languageLevel)
    if (sdkApplicable is Err) {
      val message = "${sdkApplicable.error} ${EduPythonBundle.message("configure.python.environment.help")}"
      val validationMessage = ValidationMessage(message, ENVIRONMENT_CONFIGURATION_LINK_PYTHON)
      return SettingsValidationResult.Ready(validationMessage)
    }

    return SettingsValidationResult.OK
  }

  private val Sdk.languageLevel: LanguageLevel
    get() {
      return if (sdkType === PyFakeSdkType) {
        val pythonVersion = versionString
        if (pythonVersion == null) {
          LanguageLevel.getDefault()
        }
        else {
          LanguageLevel.fromPythonVersion(pythonVersion) ?: LanguageLevel.getDefault()
        }
      }
      else {
        PythonSdkType.getLanguageLevelForSdk(this)
      }
    }

  companion object {

    private val OK = Ok(Unit)

    private fun isSdkApplicable(course: Course, sdkLanguageLevel: LanguageLevel): Result<Unit, String> {
      val courseLanguageVersion = course.languageVersion
      val isPython2Sdk = sdkLanguageLevel.isPython2

      return when (courseLanguageVersion) {
        null, ALL_VERSIONS -> OK
        PYTHON_2_VERSION -> if (isPython2Sdk) OK else NoApplicablePythonError(2)
        PYTHON_3_VERSION -> if (!isPython2Sdk) OK else NoApplicablePythonError(3)
        else -> {
          val courseLanguageLevel = LanguageLevel.fromPythonVersion(courseLanguageVersion)
          when {
            courseLanguageLevel?.isPython2 != isPython2Sdk -> SpecificPythonRequiredError(courseLanguageVersion)
            sdkLanguageLevel.isAtLeast(courseLanguageLevel) -> OK
            else -> SpecificPythonRequiredError(courseLanguageVersion)
          }
        }
      }
    }

    @JvmOverloads
    @JvmStatic
    fun getBaseSdk(course: Course, context: UserDataHolder? = null): PyBaseSdkDescriptor? {
      val baseSdks = PyBaseSdksProvider.getBaseSdks(context)
      if (baseSdks.isEmpty()) {
        return null
      }
      return baseSdks.filter { isSdkApplicable(course, it.languageLevel) == OK }.maxByOrNull { it.version }
    }

    private class NoApplicablePythonError(requiredVersion: Int,
                                          errorMessage: @Nls String = EduPythonBundle.message("error.incorrect.python",
                                                                                              requiredVersion)) : Err<String>(errorMessage)

    private class SpecificPythonRequiredError(requiredVersion: String,
                                              errorMessage: @Nls String = EduPythonBundle.message("error.old.python",
                                                                                                  requiredVersion)) : Err<String>(
      errorMessage)


    private fun createFakeSdk(course: Course, context: UserDataHolder? = null): Sdk? {
      val baseSdk = getBaseSdk(course, context) ?: return null
      val flavor = PythonSdkFlavor.getApplicableFlavors(false)[0]
      val prefix = flavor.name + " "
      val version = baseSdk.version
      if (prefix !in version) {
        return null
      }
      val pythonVersion = version.substring(prefix.length)
      val name = "new virtual env $pythonVersion"
      return ProjectJdkImpl(name, PyFakeSdkType, "", pythonVersion)
    }

    const val ALL_VERSIONS = "All versions"

    @JvmStatic
    fun installSdk(sdkToInstall: PySdkToInstall) = sdkToInstall.install(null) { detectSystemWideSdks(null, emptyList()) }
  }
}
