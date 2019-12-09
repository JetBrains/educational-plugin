package com.jetbrains.edu.go

import com.goide.GoConstants.SDK_TYPE_ID
import com.goide.sdk.GoSdk
import com.goide.sdk.combobox.GoSdkChooserCombo
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.UserDataHolder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.ValidationMessage
import java.awt.BorderLayout
import javax.swing.JComponent

class GoLanguageSettings : LanguageSettings<GoProjectSettings>() {
  private val sdkChooser: GoSdkChooserCombo = GoSdkChooserCombo()

  init {
    sdkChooser.addChangedListener {
      notifyListeners()
    }
  }

  override fun getSettings(): GoProjectSettings = GoProjectSettings(sdkChooser.sdk)

  override fun getLanguageSettingsComponents(course: Course, context: UserDataHolder?): List<LabeledComponent<JComponent>> {
    return listOf(LabeledComponent.create(sdkChooser as JComponent, SDK_TYPE_ID, BorderLayout.WEST))
  }

  override fun validate(course: Course?, courseLocation: String?): ValidationMessage? {
    if (sdkChooser.sdk == GoSdk.NULL || !sdkChooser.sdk.isValid) return ValidationMessage("Please specify $SDK_TYPE_ID")
    val message = validateSelectedSdk(sdkChooser) ?: return null
    return ValidationMessage(message)
  }
}
