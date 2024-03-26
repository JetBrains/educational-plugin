package com.jetbrains.edu.go

import com.goide.GoConstants.SDK_TYPE_ID
import com.goide.sdk.GoSdk
import com.goide.sdk.combobox.GoSdkChooserCombo
import com.intellij.facet.ui.ValidationResult
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.io.FileUtil.isAncestor
import com.jetbrains.edu.go.messages.EduGoBundle
import com.jetbrains.edu.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_GO
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.errors.SettingsValidationResult
import com.jetbrains.edu.learning.newproject.ui.errors.ValidationMessage
import java.awt.BorderLayout
import javax.swing.JComponent

class GoLanguageSettings : LanguageSettings<GoProjectSettings>() {

  private var selectedSdk: GoSdk? = null

  override fun getSettings(): GoProjectSettings = GoProjectSettings(selectedSdk ?: GoSdk.NULL)

  override fun getLanguageSettingsComponents(
    course: Course,
    disposable: CheckedDisposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> {
    val sdkChooser = GoSdkChooserCombo({ null }, { true }, { ValidationResult.OK })
    Disposer.register(disposable, sdkChooser)

    selectedSdk = sdkChooser.sdk
    sdkChooser.addChangedListener {
      selectedSdk = sdkChooser.sdk
      notifyListeners()
    }

    return listOf(LabeledComponent.create(sdkChooser as JComponent, SDK_TYPE_ID, BorderLayout.WEST))
  }

  override fun validate(course: Course?, courseLocation: String?): SettingsValidationResult {
    val sdk = selectedSdk ?: return SettingsValidationResult.Pending

    val message =  when {
      sdk == GoSdk.NULL -> ValidationMessage(EduGoBundle.message("error.no.sdk", ""), ENVIRONMENT_CONFIGURATION_LINK_GO)
      !sdk.isValid -> ValidationMessage(EduGoBundle.message("error.invalid.sdk"), ENVIRONMENT_CONFIGURATION_LINK_GO)
      courseLocation != null && isAncestor(courseLocation, sdk.homePath, false) ->
        ValidationMessage(EduGoBundle.message("error.invalid.sdk.location"), ENVIRONMENT_CONFIGURATION_LINK_GO)
      else -> null
    }

    return SettingsValidationResult.Ready(message)
  }
}
