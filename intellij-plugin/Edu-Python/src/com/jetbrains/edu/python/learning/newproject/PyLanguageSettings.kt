package com.jetbrains.edu.python.learning.newproject

import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.UserDataHolder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.ModalityStateProvider
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.errors.SettingsValidationResult
import javax.swing.JComponent


open class PyLanguageSettings : LanguageSettings<PyProjectSettings>() {

  private val projectSettings: PyProjectSettings = PyProjectSettings()
  private var isSettingsInitialized = false

  override fun getLanguageSettingsComponents(
    course: Course,
    modalityStateProvider: ModalityStateProvider,
    disposable: CheckedDisposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> {

    val component = getLanguageSettingsComponents(course, disposable, context, projectSettings, ::notifyListeners)
    isSettingsInitialized = true
    return component
  }

  // Inspired by `com.jetbrains.python.sdk.add.PyAddSdkPanelKt.addBaseInterpretersAsync` implementation
  /**
   * @return list of all available python interpreters and a recommended one to select
   */

  override fun getSettings(): PyProjectSettings = projectSettings

  override fun validate(course: Course?, courseLocation: String?): SettingsValidationResult {
    course ?: return SettingsValidationResult.OK
    return validate(isSettingsInitialized, projectSettings, course)
  }

  companion object {
    const val ALL_VERSIONS = "All versions"
  }
}
