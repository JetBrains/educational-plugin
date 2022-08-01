package com.jetbrains.edu.python.learning.newproject

import com.intellij.openapi.Disposable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.UserDataHolder
import com.jetbrains.edu.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_PYTHON
import com.jetbrains.edu.learning.EduNames.PYTHON_2_VERSION
import com.jetbrains.edu.learning.EduNames.PYTHON_3_VERSION
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.ValidationMessage
import com.jetbrains.edu.python.learning.messages.EduPythonBundle
import com.jetbrains.python.newProject.PyNewProjectSettings
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import javax.swing.JComponent

open class PyLanguageSettings : LanguageSettings<PyNewProjectSettings>() {

  private val mySettings: PyNewProjectSettings = PyNewProjectSettings()

  override fun getLanguageSettingsComponents(
    course: Course,
    disposable: Disposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> {
    // by default, we create new virtual env in project, we need to add this non-existing sdk to sdk list
    val fakeSdk = createFakeSdk(course, context)

    val combo = getInterpreterComboBox(fakeSdk)
    return listOf<LabeledComponent<JComponent>>(
      LabeledComponent.create(combo, EduCoreBundle.message("select.interpreter"), BorderLayout.WEST))
  }

  override fun getSettings(): PyNewProjectSettings = mySettings

  override fun validate(course: Course?, courseLocation: String?): ValidationMessage? {
    course ?: return null
    val sdk = mySettings.sdk ?: return ValidationMessage(EduPythonBundle.message("error.no.python.interpreter", ""),
                                                         ENVIRONMENT_CONFIGURATION_LINK_PYTHON)

    return (isSdkApplicable(course, sdk.languageLevel) as? Err)?.error?.let {
      val message = "$it ${EduPythonBundle.message("configure.python.environment.help")}"
      ValidationMessage(message, ENVIRONMENT_CONFIGURATION_LINK_PYTHON)
    }
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

  private fun getInterpreterComboBox(fakeSdk: Sdk?): JComponent {
    val helper = PySdkSettingsHelper.firstAvailable()
    return helper.getInterpreterComboBox(fakeSdk) { sdk ->
      mySettings.sdk = sdk
      notifyListeners()
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

    private fun createFakeSdk(course: Course, context: UserDataHolder?): ProjectJdkImpl? {
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
  }
}
