package com.jetbrains.edu.learning

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.UserDataHolder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.ValidationMessage
import javax.swing.JComponent

/**
 * Main interface responsible for course project language settings such as JDK or interpreter
 *
 * @param Settings container type holds project settings state
 */
abstract class LanguageSettings<Settings : Any> {
  protected val listeners: MutableSet<SettingsChangeListener> = mutableSetOf()

  /**
   * Returns list of UI components that allows user to select course project settings such as project JDK or interpreter.
   *
   * @param course course of creating project
   * @param context used as cache. If provided, must have "session"-scope. Session could be one dialog or wizard.
   * @return list of UI components with project settings
   *
   * @see PyLanguageSettings
   */
  open fun getLanguageSettingsComponents(
    course: Course,
    disposable: Disposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> {
    return emptyList()
  }

  /**
   * If [listener] is already added, it won't be added again
   */
  fun addSettingsChangeListener(listener: SettingsChangeListener) {
    listeners.add(listener)
  }

  open fun validate(course: Course?, courseLocation: String?): ValidationMessage? {
    return null
  }

  /**
   * Returns project settings associated with state of language settings UI component.
   * It should be passed into project generator to set chosen settings in course project.
   *
   * @return project settings object
   */
  abstract fun getSettings(): Settings

  /**
   * Returns string representations of all possible language versions to be shown to a user
   */
  open fun getLanguageVersions(): List<String> = emptyList()

  protected fun notifyListeners() {
    for (listener in listeners) {
      listener.settingsChanged()
    }
  }

  fun interface SettingsChangeListener {
    fun settingsChanged()
  }
}
