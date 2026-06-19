package com.jetbrains.edu.python.learning.newproject

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.jetbrains.edu.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_PYTHON
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PYTHON_2_VERSION
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PYTHON_3_VERSION
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.errors.SettingsValidationResult
import com.jetbrains.edu.learning.newproject.ui.errors.ValidationMessage
import com.jetbrains.edu.learning.newproject.ui.errors.ready
import com.jetbrains.edu.python.learning.messages.EduPythonBundle
import com.jetbrains.edu.python.learning.newproject.PyLanguageSettings.Companion.ALL_VERSIONS
import com.jetbrains.python.PythonRuntimeService
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.sdk.*
import com.jetbrains.python.sdk.add.PySdkPathChoosingComboBox
import com.jetbrains.python.sdk.add.addInterpretersAsync
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import javax.swing.JComponent

typealias PySdkType = Sdk

fun getLanguageSettingsComponents(
  course: Course,
  disposable: CheckedDisposable,
  context: UserDataHolder?,
  projectSettings: PyProjectSettings,
  notifyListeners: () -> Unit,
): List<LabeledComponent<JComponent>> {
  val sdkField = PySdkPathChoosingComboBox()
  sdkField.childComponent.addItemListener {
    if (it.stateChange == ItemEvent.SELECTED) {
      projectSettings.sdk = sdkField.selectedSdk
      notifyListeners()
    }
  }

  // Suggested to be replaced with PythonInterpreterCombobox, but PythonInterpreterCombobox is currently internal
  // TODO: use PythonInterpreterCombobox when instead
  var recommendedSdk: Sdk? = null

  @Suppress("DEPRECATION_ERROR")
  addInterpretersAsync(sdkField, {
    val (collectedSdks, sdkToSelect) = collectPySdks(course, context ?: UserDataHolderBase())
    recommendedSdk = sdkToSelect
    collectedSdks
  }) {
    if (disposable.isDisposed) return@addInterpretersAsync

    recommendedSdk?.let { sdkField.selectedSdk = it }
    projectSettings.sdk = sdkField.selectedSdk
    notifyListeners()
  }

  return listOf(
    LabeledComponent.create(sdkField, EduCoreBundle.message("select.interpreter"), BorderLayout.WEST)
  )
}

@RequiresBackgroundThread
private fun collectPySdks(course: Course, context: UserDataHolder): Pair<List<Sdk>, Sdk?> {
  val fakeSdk = createFakeSdk(course, context)
  val fakeSdks = listOfNotNull(fakeSdk)

  val sdks = (fakeSdks + findBaseSdks(emptyList(), null, context))
    // It's important to check validity here, in background thread,
    // because it caches a result of checking if python binary is executable.
    // If the first (uncached) invocation is invoked in EDT, it may throw exception and break UI rendering.
    // See https://youtrack.jetbrains.com/issue/EDU-6371
    .filter { it.sdkSeemsValid }

  val needSdksToInstall = sdks.all { OK != isSdkApplicable(course, it.languageLevel) }

  val collectedSdks = if (needSdksToInstall) {
    sdks + getSdksToInstall()
  }
  else {
    sdks
  }

  val recommendedSdk = fakeSdk ?: collectedSdks
    .filter { isSdkApplicable(course, it.languageLevel) == OK }
    .maxByOrNull { it.languageLevel }

  return collectedSdks to recommendedSdk
}

private val Sdk.languageLevel: LanguageLevel
  get() {
    return when (this) {
      is PySdkToCreateVirtualEnv -> {
        val pythonVersion = versionString
        if (pythonVersion == null) {
          LanguageLevel.getDefault()
        }
        else {
          LanguageLevel.fromPythonVersion(pythonVersion) ?: LanguageLevel.getDefault()
        }
      }

      is PyDetectedSdk -> {
        // PyDetectedSdk has empty `sdk.versionString`, so we should manually get language level from homePath if it exists
        homePath?.let {
          sdkFlavor.getLanguageLevel(it)
        } ?: LanguageLevel.getDefault()
      }

      is PySdkToInstall -> {
        LanguageLevel.fromPythonVersion(installation.release.version) ?: LanguageLevel.getDefault()
      }

      else -> {
        PythonRuntimeService.getInstance().getLanguageLevelForSdk(this)
      }
    }
  }

fun validate(isSettingsInitialized: Boolean, projectSettings: PyProjectSettings, course: Course): SettingsValidationResult {
  val sdk = projectSettings.sdk ?: return if (isSettingsInitialized) {
    ValidationMessage(
      EduPythonBundle.message("error.no.python.interpreter", ENVIRONMENT_CONFIGURATION_LINK_PYTHON),
      ENVIRONMENT_CONFIGURATION_LINK_PYTHON
    ).ready()
  }
  else {
    SettingsValidationResult.Pending
  }

  val sdkApplicable = isSdkApplicable(course, sdk.languageLevel)
  if (sdkApplicable is Err) {
    val message = "${sdkApplicable.error}<br>${EduPythonBundle.message("configure.python.environment.help")}"
    val validationMessage = ValidationMessage(message, ENVIRONMENT_CONFIGURATION_LINK_PYTHON)
    return SettingsValidationResult.Ready(validationMessage)
  }

  return SettingsValidationResult.OK
}

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

@RequiresBackgroundThread
private fun getBaseSdk(course: Course, context: UserDataHolder? = null): PyBaseSdkDescriptor? {
  val baseSdks = PyBaseSdksProvider.getBaseSdks(context)
  if (baseSdks.isEmpty()) {
    return null
  }
  return baseSdks.filter { isSdkApplicable(course, it.languageLevel) == OK }.maxByOrNull { it.languageLevel }
}

private class NoApplicablePythonError(
  requiredVersion: Int,
  errorMessage: @Nls String = EduPythonBundle.message(
    "error.incorrect.python",
    requiredVersion
  )
) : Err<String>(errorMessage)

private class SpecificPythonRequiredError(
  requiredVersion: String,
  errorMessage: @Nls String = EduPythonBundle.message(
    "error.old.python",
    requiredVersion
  )
) : Err<String>(
  errorMessage
)

@RequiresBackgroundThread
private fun createFakeSdk(course: Course, context: UserDataHolder): Sdk? {
  val baseSdk = getBaseSdk(course, context) ?: return null
  val flavor = PythonSdkFlavor.getApplicableFlavors(false)[0]
  val prefix = flavor.name + " "
  val version = baseSdk.version
  if (prefix !in version) {
    return null
  }
  val pythonVersion = version.substring(prefix.length)
  val name = "new virtual env $pythonVersion"

  return PySdkToCreateVirtualEnv.create(name, baseSdk.path, pythonVersion)
}

private val OK = Ok(Unit)
