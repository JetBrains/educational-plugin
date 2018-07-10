package com.jetbrains.edu.android

import com.android.tools.idea.sdk.IdeSdks
import com.android.tools.idea.ui.ApplicationUtils
import com.android.tools.idea.welcome.config.FirstRunWizardMode
import com.android.tools.idea.welcome.install.FirstRunWizardDefaults
import com.android.tools.idea.welcome.wizard.deprecated.ConsolidatedProgressStep
import com.android.tools.idea.welcome.wizard.deprecated.InstallComponentsPath
import com.android.tools.idea.wizard.WizardConstants
import com.android.tools.idea.wizard.dynamic.DialogWrapperHost
import com.android.tools.idea.wizard.dynamic.DynamicWizard
import com.android.tools.idea.wizard.dynamic.DynamicWizardHost
import com.android.tools.idea.wizard.dynamic.SingleStepPath
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.intellij.JdkLanguageSettings
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.File
import javax.swing.JComponent

class AndroidLanguageSettings : JdkLanguageSettings(), ActionListener {

  private val locationField = TextFieldWithBrowseButton()

  init {
    locationField.text = IdeSdks.getInstance().androidSdkPath?.absolutePath ?: ""
    locationField.isEditable = false
    locationField.addActionListener(this)
  }

  override fun getLanguageSettingsComponents(course: Course): List<LabeledComponent<JComponent>> {
    val androidSdkLocation = LabeledComponent.create<JComponent>(locationField, "Android SDK Location", BorderLayout.WEST)
    return super.getLanguageSettingsComponents(course) + androidSdkLocation
  }

  override fun validate(): String? {
    return if (locationField.text.isEmpty()) "Specify Android SDK location" else null
  }

  // Inspired by [com.android.tools.idea.updater.configure.SdkUpdaterConfigPanel#setUpSingleSdkChooser]
  override fun actionPerformed(e: ActionEvent) {
    val host = DialogWrapperHost(null)
    val wizard = object : DynamicWizard(null, null, "SDK Setup", host) {
      override fun init() {
        val progressStep = DownloadingComponentsStep(myHost.disposable, myHost)

        val sdkPath = locationField.text
        val location = if (sdkPath.isEmpty()) {
          FirstRunWizardDefaults.getInitialSdkLocation(FirstRunWizardMode.MISSING_SDK)
        } else {
          File(sdkPath)
        }

        val path = InstallComponentsPath(FirstRunWizardMode.MISSING_SDK, location, progressStep, false)

        progressStep.setInstallComponentsPath(path)

        addPath(path)
        addPath(SingleStepPath(progressStep))
        super.init()
      }

      override fun performFinishingActions() {
        val stateSdkLocationPath = myState.get(WizardConstants.KEY_SDK_INSTALL_LOCATION) ?: return
        locationField.text = stateSdkLocationPath
        notifyListeners()
        val stateSdkLocation = File(stateSdkLocationPath)
        if (!FileUtil.filesEqual(IdeSdks.getInstance().androidSdkPath, stateSdkLocation)) {
          setAndroidSdkLocation(stateSdkLocation)

        }
      }

      override fun getProgressTitle(): String = "Setting up SDK..."
      override fun getWizardActionDescription(): String = "Setting up SDK..."
    }
    wizard.init()
    wizard.show()
  }

  private fun setAndroidSdkLocation(sdkLocation: File) {
    ApplicationUtils.invokeWriteActionAndWait(ModalityState.any()) {
      // TODO Do we have to pass the default project here too instead of null?
      IdeSdks.getInstance().setAndroidSdkPath(sdkLocation, null)
    }
  }

  private class DownloadingComponentsStep(
    disposable: Disposable,
    host: DynamicWizardHost
  ) : ConsolidatedProgressStep(disposable, host) {

    private var installComponentsPath: InstallComponentsPath? = null

    fun setInstallComponentsPath(path: InstallComponentsPath) {
      setPaths(listOf(path))
      installComponentsPath = path
    }

    override fun isStepVisible(): Boolean {
      return installComponentsPath?.shouldDownloadingComponentsStepBeShown() == true
    }
  }
}
