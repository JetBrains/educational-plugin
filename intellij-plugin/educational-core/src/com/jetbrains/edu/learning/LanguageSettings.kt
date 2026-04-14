package com.jetbrains.edu.learning

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolder
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.EduProjectSettings
import com.jetbrains.edu.learning.newproject.ui.errors.SettingsValidationResult
import java.awt.Component
import java.awt.Dialog
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import javax.swing.JComponent
import javax.swing.SwingUtilities

/**
 * Main interface responsible for course project language settings such as JDK or interpreter
 *
 * @param Settings container type holds project settings state
 */
abstract class LanguageSettings<Settings : EduProjectSettings> {
  private val listeners: MutableSet<SettingsChangeListener> = mutableSetOf()

  /**
   * Returns list of UI components that allows user to select course project settings such as project JDK or interpreter.
   *
   * @param course course of creating project
   * @param context used as cache. If provided, must have "session"-scope. Session could be one dialog or wizard.
   * @return list of UI components with project settings
   *
   * @see PyLanguageSettings
   */
  @RequiresEdt
  open fun getLanguageSettingsComponents(
    course: Course,
    disposable: CheckedDisposable,
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

  open fun validate(course: Course?, courseLocation: String?): SettingsValidationResult {
    return SettingsValidationResult.OK
  }

  /**
   * Returns project settings associated with state of language settings UI component.
   * It should be passed into project generator to set chosen settings in course project.
   *
   * @return project settings object
   */
  abstract fun getSettings(): Settings

  protected fun notifyListeners() {
    for (listener in listeners) {
      listener.settingsChanged()
    }
  }

  fun interface SettingsChangeListener {
    fun settingsChanged()
  }

  /**
   * Helper method. Waits until the component becomes available inside a visible modal dialog and runs action
   * with the modality of that dialog.
   */
  protected fun waitForModality(component: Component, disposable: Disposable, action: (ModalityState) -> Unit) {
    fun Component.isOnVisibleDialog(): Boolean {
      val window = SwingUtilities.getWindowAncestor(this) ?: return false
      return window is Dialog && window.isShowing
    }

    if (component.isOnVisibleDialog()) {
      action(ModalityState.stateForComponent(component))
      return
    }

    val hierarchyListener = object : HierarchyListener {
      override fun hierarchyChanged(e: HierarchyEvent?) {
        if (component.isOnVisibleDialog()) {
          action(ModalityState.stateForComponent(component))
          component.removeHierarchyListener(this)
        }
      }
    }

    Disposer.register(disposable) {
      runInEdt {
        component.removeHierarchyListener(hierarchyListener)
      }
    }

    component.addHierarchyListener(hierarchyListener)
  }
}