package com.jetbrains.edu.python.learning.newproject

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.jetbrains.edu.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_PYTHON
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.LanguageSettings
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
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.sdk.PyDetectedSdk
import com.jetbrains.python.sdk.PySdkToInstall
import com.jetbrains.python.sdk.add.PySdkPathChoosingComboBox
import com.jetbrains.python.sdk.add.addInterpretersAsync
import com.jetbrains.python.sdk.findBaseSdks
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor.getLanguageLevelFromVersionStringStatic
import com.jetbrains.python.sdk.getSdksToInstall
import com.jetbrains.python.sdk.sdkFlavor
import com.jetbrains.python.sdk.sdkSeemsValid
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.detectSystemWideSdks
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import javax.swing.JComponent

open class PyLanguageSettings : LanguageSettings<PyProjectSettings>() {

  private val projectSettings: PyProjectSettings = PyProjectSettings()
  private var isSettingsInitialized = false

  override fun getLanguageSettingsComponents(
    course: Course,
    disposable: CheckedDisposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> {

    val sdkField = PySdkPathChoosingComboBox()
    sdkField.childComponent.addItemListener {
      if (it.stateChange == ItemEvent.SELECTED) {
        projectSettings.sdk = sdkField.selectedSdk
        notifyListeners()
      }
    }

    addInterpretersAsync(sdkField, {
      collectPySdks(course, context ?: UserDataHolderBase())
    }) {
      if (disposable.isDisposed) return@addInterpretersAsync
      projectSettings.sdk = sdkField.selectedSdk
      isSettingsInitialized = true
      notifyListeners()
    }

    return listOf<LabeledComponent<JComponent>>(
      LabeledComponent.create(sdkField, EduCoreBundle.message("select.interpreter"), BorderLayout.WEST)
    )
  }

  // Inspired by `com.jetbrains.python.sdk.add.PyAddSdkPanelKt.addBaseInterpretersAsync` implementation
  @RequiresBackgroundThread
  private fun collectPySdks(course: Course, context: UserDataHolder): List<Sdk> {
    val fakeSdk = listOfNotNull(createFakeSdk(course, context))

    return (fakeSdk + findBaseSdks(emptyList(), null, context))
      // It's important to check validity here, in background thread,
      // because it caches a result of checking if python binary is executable.
      // If the first (uncached) invocation is invoked in EDT, it may throw exception and break UI rendering.
      // See https://youtrack.jetbrains.com/issue/EDU-6371
      .filter { it.sdkSeemsValid }
      .takeIf { it.isNotEmpty() }
      ?: getSdksToInstall()
  }

  override fun getSettings(): PyProjectSettings = projectSettings

  override fun validate(course: Course?, courseLocation: String?): SettingsValidationResult {
    course ?: return SettingsValidationResult.OK
    val sdk = projectSettings.sdk ?: return if (isSettingsInitialized) {
      ValidationMessage(EduPythonBundle.message("error.no.python.interpreter", ENVIRONMENT_CONFIGURATION_LINK_PYTHON),
                        ENVIRONMENT_CONFIGURATION_LINK_PYTHON).ready()
    }
    else {
      SettingsValidationResult.Pending
    }

    val sdkApplicable = isSdkApplicable(course, sdk.languageLevel)
    if (sdkApplicable is Err) {
      val message = "${sdkApplicable.error}<br>${EduPythonBundle.message("configure.python.environment.help")}"

      // Extract the required version from the error message
      val requiredVersion = when (sdkApplicable) {
        is NoApplicablePythonError -> sdkApplicable.requiredVersion.toString()
        is SpecificPythonRequiredError -> sdkApplicable.requiredVersion
        else -> null
      }

      // Add download button if we have a required version
      val validationMessage = if (requiredVersion != null) {
        val buttonText = EduPythonBundle.message("action.download.python", requiredVersion)
        ValidationMessage(
          message = message,
          hyperlinkAddress = ENVIRONMENT_CONFIGURATION_LINK_PYTHON,
          actionButtonText = buttonText,
          action = { downloadPythonSdk(requiredVersion, null) { sdk ->
            if (sdk != null) {
              projectSettings.sdk = sdk
              notifyListeners()
            }
          }}
        )
      } else {
        ValidationMessage(message, ENVIRONMENT_CONFIGURATION_LINK_PYTHON)
      }

      return SettingsValidationResult.Ready(validationMessage)
    }

    return SettingsValidationResult.OK
  }

  private val Sdk.languageLevel: LanguageLevel
    get() {
      return when(this) {
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
        else -> {
          // Get version string and convert it to language level
          val versionString = versionString
          if (versionString != null) {
            getLanguageLevelFromVersionStringStatic(versionString)
          } else {
            LanguageLevel.getDefault()
          }
        }
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

    @RequiresBackgroundThread
    private fun getBaseSdk(course: Course, context: UserDataHolder? = null): PyBaseSdkDescriptor? {
      val baseSdks = PyBaseSdksProvider.getBaseSdks(context)
      if (baseSdks.isEmpty()) {
        return null
      }
      return baseSdks.filter { isSdkApplicable(course, it.languageLevel) == OK }.maxByOrNull { it.languageLevel }
    }

    private class NoApplicablePythonError(val requiredVersion: Int,
                                          errorMessage: @Nls String = EduPythonBundle.message("error.incorrect.python",
                                                                                              requiredVersion)) : Err<String>(errorMessage)

    private class SpecificPythonRequiredError(val requiredVersion: String,
                                              errorMessage: @Nls String = EduPythonBundle.message("error.old.python",
                                                                                                  requiredVersion)) : Err<String>(
      errorMessage)

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

    /**
     * Downloads and configures a Python SDK for the specified version.
     * 
     * @param requiredVersion The required Python version (e.g., "3.8")
     * @param project The current project
     * @param onComplete Callback to be called when the download is complete
     */
    fun downloadPythonSdk(requiredVersion: String, project: Project?, onComplete: (Sdk?) -> Unit) {
      val sdksToInstall = getSdksToInstall().filter { sdk -> 
        // Filter SDKs that match the required version
        // The name typically contains the version information
        sdk.name.contains(requiredVersion)
      }

      if (sdksToInstall.isEmpty()) {
        Messages.showErrorDialog(
          project,
          EduPythonBundle.message("error.no.python.to.download", requiredVersion),
          EduPythonBundle.message("error.download.failed")
        )
        onComplete(null)
        return
      }

      val sdkToInstall = sdksToInstall.first()

      ProgressManager.getInstance().run(object : Task.Backgroundable(
        project,
        EduPythonBundle.message("progress.downloading.python", sdkToInstall.name),
        false
      ) {
        override fun run(indicator: ProgressIndicator) {
          indicator.isIndeterminate = true
          indicator.text = EduPythonBundle.message("progress.downloading.python", sdkToInstall.name)

          try {
            // Install the SDK using the Python plugin's functionality
            // This must be done on the EDT
            @Suppress("UnstableApiUsage")
            val installedSdk = invokeAndWaitIfNeeded(ModalityState.any()) {
              sdkToInstall.install(null) {
                detectSystemWideSdks(null, emptyList())
              }.getOrElse {
                throw RuntimeException("Failed to install SDK: ${it.message}", it)
              }
            }

            if (installedSdk != null) {
              ApplicationManager.getApplication().invokeLater({
                onComplete(installedSdk)
              }, ModalityState.any())
            } else {
              throw RuntimeException("Failed to create SDK")
            }
          }
          catch (e: Exception) {
            ApplicationManager.getApplication().invokeLater({
              Messages.showErrorDialog(
                project,
                EduPythonBundle.message("error.download.failed.with.reason", e.message ?: ""),
                EduPythonBundle.message("error.download.failed")
              )
              onComplete(null)
            }, ModalityState.any())
          }
        }
      })
    }

    const val ALL_VERSIONS = "All versions"
  }
}
