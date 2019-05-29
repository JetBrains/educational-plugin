package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapperPeer
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.HideableDecorator
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

open class CCCreateAnswerPlaceholderDialog(
  project: Project,
  placeholderText: String,
  isEdit: Boolean,
  private val placeholder: AnswerPlaceholder
) : DialogWrapper(project, true) {
  private val panel: CCAddAnswerPlaceholderPanel = CCAddAnswerPlaceholderPanel(placeholderText)
  // "30" is the same value of text field columns as Messages.InputDialog uses
  private val dependencyPathField: JBTextField = JBTextField(30)
  private val visibilityCheckBox: JBCheckBox = JBCheckBox("Visible", placeholder.placeholderDependency?.isVisible == true)
  private val pathLabel: JLabel = JLabel("[sectionName#]lessonName#taskName#filePath#placeholderIndex")
  private val isFirstTask: Boolean = placeholder.taskFile.task.isFirstInCourse
  private val currentText: String get() = dependencyPathField.text ?: ""
  private val taskText: String = StringUtil.notNullize(panel.getAnswerPlaceholderText()).trim { it <= ' ' }

  init {
    this.title = (if (isEdit) "Edit" else "Add") + TITLE_SUFFIX
    val buttonText = if (isEdit) "OK" else "Add"
    setOKButtonText(buttonText)
    super.init()
    initValidation()
  }

  override fun createCenterPanel(): JComponent {
    dependencyPathField.putClientProperty(DialogWrapperPeer.HAVE_INITIAL_SELECTION, true)
    pathLabel.foreground = JBColor.GRAY

    if (!isFirstTask) {
      val dependencyPanel = JPanel(BorderLayout())
      val contentPanel = panel {
        row { dependencyPathField() }
        row { pathLabel() }
        row { visibilityCheckBox() }
      }
      contentPanel.border = JBUI.Borders.emptyBottom(5)
      val decorator = HideableDecorator(dependencyPanel, "Add Answer Placeholder Dependency", true)
      decorator.setContentComponent(contentPanel)
      if (placeholder.placeholderDependency != null) {
        decorator.setOn(true)
        dependencyPathField.text = placeholder.placeholderDependency?.toString()
      }

      dependencyPanel.alignmentX = Component.LEFT_ALIGNMENT
      dependencyPanel.maximumSize = Dimension(Int.MAX_VALUE, 0)
      panel.add(dependencyPanel)
    }
    return panel
  }

  override fun doValidate(): ValidationInfo? {
    if (currentText.isEmpty()) {
      return null
    }
    val errorText = try {
      val dependency = AnswerPlaceholderDependency.create(placeholder, currentText)
      if (dependency == null) "Invalid dependency" else null
    }
    catch (e: AnswerPlaceholderDependency.InvalidDependencyException) {
      e.customMessage
    }
    return if (errorText != null) ValidationInfo(errorText) else null
  }

  override fun getPreferredFocusedComponent(): JComponent? {
    return panel.getPreferredFocusedComponent()
  }

  open fun getTaskText(): String = taskText

  fun getDependencyInfo(): DependencyInfo? =
    if (!(currentText.isBlank() || isFirstTask)) {
      DependencyInfo(currentText, visibilityCheckBox.isSelected)
    }
    else null

  data class DependencyInfo(val dependencyPath: String, val isVisible: Boolean)

  companion object {
    private const val TITLE_SUFFIX = " Answer Placeholder"
  }
}

private val Task.isFirstInCourse: Boolean
  get() {
    if (index > 1) {
      return false
    }
    val section = lesson.section ?: return lesson.index == 1
    return section.index == 1 && lesson.index == 1
  }