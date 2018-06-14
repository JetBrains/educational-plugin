package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapperPeer
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.*
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency
import javax.swing.JComponent

class CCDependencyDialog(
  project: Project,
  private val placeholder: AnswerPlaceholder
) : DialogWrapper(project) {

  // "30" is the same value of text field columns as Messages.InputDialog uses
  private val dependencyPathField = JBTextField(initialValue(placeholder), 30)
  private val visibilityCheckBox = JBCheckBox("Visible", placeholder.placeholderDependency?.isVisible == true)

  private val currentText: String get() = dependencyPathField.text ?: ""

  init {
    title = CCAddDependency.getActionName(placeholder)
    init()
  }

  override fun createCenterPanel(): JComponent {
    val text = currentText
    dependencyPathField.select(0, text.length)
    dependencyPathField.putClientProperty(DialogWrapperPeer.HAVE_INITIAL_SELECTION, true)
    return panel {
      row { dependencyPathField() }
      row { visibilityCheckBox() }
    }
  }

  override fun getPreferredFocusedComponent(): JComponent? = dependencyPathField

  override fun postponeValidation(): Boolean = false

  override fun doValidate(): ValidationInfo? {
    val errorText = try {
      val dependency = AnswerPlaceholderDependency.create(placeholder, currentText)
      if (dependency == null) "invalid dependency" else null
    } catch (e: AnswerPlaceholderDependency.InvalidDependencyException) {
      e.customMessage
    }
    return if (errorText != null) ValidationInfo(errorText) else null
  }

  fun showAndGetResult(): DependencyInfo? {
    val isOk = showAndGet()
    return if (isOk) DependencyInfo(currentText, visibilityCheckBox.isSelected) else null
  }

  private fun initialValue(placeholder: AnswerPlaceholder) : String {
    // TODO: come up with better initial value
    return placeholder.placeholderDependency?.toString() ?: "lesson1#task1#path/task.txt#1"
  }

  data class DependencyInfo(val dependencyPath: String, val isVisible: Boolean)
}
