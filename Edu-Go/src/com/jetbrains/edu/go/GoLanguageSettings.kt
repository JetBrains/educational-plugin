package com.jetbrains.edu.go

import com.goide.GoConstants.GO_ROOT
import com.goide.sdk.GoSdk
import com.goide.sdk.combobox.GoSdkChooserCombo
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.UserDataHolder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import java.awt.BorderLayout
import javax.swing.JComponent

class GoLanguageSettings : LanguageSettings<GoProjectSettings>() {
  private val sdkChooser: GoSdkChooserCombo = GoSdkChooserCombo()

  override fun getSettings(): GoProjectSettings = GoProjectSettings(GoSdk.NULL)

  override fun getLanguageSettingsComponents(course: Course, context: UserDataHolder?): MutableList<LabeledComponent<JComponent>> {
    return mutableListOf(LabeledComponent.create(sdkChooser as JComponent, GO_ROOT, BorderLayout.WEST))
  }
}
