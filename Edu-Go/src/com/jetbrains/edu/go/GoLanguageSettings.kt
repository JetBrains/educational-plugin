package com.jetbrains.edu.go

import com.goide.GoConstants.SDK_TYPE_ID
import com.goide.sdk.GoSdk
import com.goide.sdk.combobox.GoSdkChooserCombo
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.io.FileUtil.isAncestor
import com.jetbrains.edu.go.messages.EduGoBundle
import com.jetbrains.edu.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_GO
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.ValidationMessage
import java.awt.BorderLayout
import javax.swing.JComponent

class GoLanguageSettings : LanguageSettings<GoProjectSettings>() {

  private var selectedSdk: GoSdk? = null

  override fun getSettings(): GoProjectSettings = GoProjectSettings(selectedSdk ?: GoSdk.NULL)

  override fun getLanguageSettingsComponents(course: Course, disposable: Disposable, context: UserDataHolder?): List<LabeledComponent<JComponent>> {
    val sdkChooser = GoSdkChooserCombo()
    Disposer.register(disposable, sdkChooser)

    selectedSdk = sdkChooser.sdk
    sdkChooser.addChangedListener {
      selectedSdk = sdkChooser.sdk
      notifyListeners()
    }

    return listOf(LabeledComponent.create(sdkChooser as JComponent, SDK_TYPE_ID, BorderLayout.WEST))
  }

  override fun validate(course: Course?, courseLocation: String?): ValidationMessage? {
    val sdk = selectedSdk ?: return null
    if (sdk == GoSdk.NULL) return ValidationMessage(EduGoBundle.message("error.no.sdk", ""), ENVIRONMENT_CONFIGURATION_LINK_GO)
    if (!sdk.isValid) return ValidationMessage(EduGoBundle.message("error.invalid.sdk"), ENVIRONMENT_CONFIGURATION_LINK_GO)
    if (courseLocation != null && isAncestor(courseLocation, sdk.homePath, false))
      return ValidationMessage(EduGoBundle.message("error.invalid.sdk.location"), ENVIRONMENT_CONFIGURATION_LINK_GO)
    return null
  }
}
