package com.jetbrains.edu.android

import com.android.tools.idea.sdk.IdeSdks
import com.android.tools.idea.ui.ApplicationUtils
import com.android.tools.idea.welcome.config.FirstRunWizardMode
import com.android.tools.idea.welcome.install.FirstRunWizardDefaults
import com.android.tools.idea.welcome.install.SdkComponentInstaller
import com.android.tools.idea.welcome.wizard.FirstRunWizardTracker
import com.android.tools.idea.welcome.wizard.deprecated.ConsolidatedProgressStep
import com.android.tools.idea.welcome.wizard.deprecated.InstallComponentsPath
import com.android.tools.idea.wizard.WizardConstants
import com.android.tools.idea.wizard.dynamic.DialogWrapperHost
import com.android.tools.idea.wizard.dynamic.DynamicWizard
import com.android.tools.idea.wizard.dynamic.DynamicWizardHost
import com.android.tools.idea.wizard.dynamic.SingleStepPath
import com.google.wireless.android.sdk.stats.SetupWizardEvent
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.android.messages.EduAndroidBundle
import com.jetbrains.edu.jvm.JdkLanguageSettings
import com.jetbrains.edu.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_ANDROID
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.errors.SettingsValidationResult
import com.jetbrains.edu.learning.newproject.ui.errors.ValidationMessage
import org.jetbrains.annotations.Nls
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

  override fun getLanguageSettingsComponents(
    course: Course,
    disposable: CheckedDisposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> {
    val androidSdkLocation = LabeledComponent.create<JComponent>(locationField, EduAndroidBundle.message("android.sdk.location"),
                                                                 BorderLayout.WEST)
    return super.getLanguageSettingsComponents(course, disposable, context) + androidSdkLocation
  }

  override fun validate(course: Course?, courseLocation: String?): SettingsValidationResult {
    val validationMessage = if (locationField.text.isEmpty()) {
      ValidationMessage(EduAndroidBundle.message("error.no.sdk"), ENVIRONMENT_CONFIGURATION_LINK_ANDROID)
    }
    else {
      null
    }

    return SettingsValidationResult.Ready(validationMessage)
  }

  // Inspired by [com.android.tools.idea.updater.configure.SdkUpdaterConfigPanel#setUpSingleSdkChooser]
  override fun actionPerformed(e: ActionEvent) {
    val host = DialogWrapperHost(null)


    val wizard = EduAndroidWizard(EduAndroidBundle.message("setup.sdk"), host)
    wizard.init()
    wizard.show()
  }

  private inner class EduAndroidWizard(
    @Nls(capitalization = Nls.Capitalization.Title) wizardName: String,
    host: DialogWrapperHost
  ) : DynamicWizard(null, null, wizardName, host) {
    override fun init() {
      val tracker = FirstRunWizardTracker(SetupWizardEvent.SetupWizardMode.MISSING_SDK, false)
      val progressStep = DownloadingComponentsStep(myHost.disposable, myHost, tracker)

      val sdkPath = locationField.text
      val location = if (sdkPath.isEmpty()) {
        FirstRunWizardDefaults.getInitialSdkLocation(FirstRunWizardMode.MISSING_SDK)
      }
      else {
        File(sdkPath)
      }

      val path = InstallComponentsPath(FirstRunWizardMode.MISSING_SDK, location, progressStep, SdkComponentInstaller(), false, tracker)

      progressStep.setInstallComponentsPath(path)

      addPath(path)
      addPath(SingleStepPath(progressStep))
      super.init()
    }

    override fun performFinishingActions() {
      val stateSdkLocationPath = myState[WizardConstants.KEY_SDK_INSTALL_LOCATION] ?: return
      locationField.text = stateSdkLocationPath
      notifyListeners()
      val stateSdkLocation = File(stateSdkLocationPath)
      if (!FileUtil.filesEqual(IdeSdks.getInstance().androidSdkPath, stateSdkLocation)) {
        setAndroidSdkLocation(stateSdkLocation)

      }
    }

    @Suppress("UnstableApiUsage")
    @NlsContexts.ProgressTitle
    override fun getProgressTitle(): String = EduAndroidBundle.message("setting.up.sdk")

    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getWizardActionDescription(): String = EduAndroidBundle.message("setting.up.sdk")
  }

  private fun setAndroidSdkLocation(sdkLocation: File) {
    ApplicationUtils.invokeWriteActionAndWait(ModalityState.any()) {
      IdeSdks.getInstance().setAndroidSdkPath(sdkLocation)
    }
  }

  private class DownloadingComponentsStep(
    disposable: Disposable,
    host: DynamicWizardHost,
    tracker: FirstRunWizardTracker
  ) : ConsolidatedProgressStep(disposable, host, tracker) {

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
