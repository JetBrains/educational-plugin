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

import javax.swing.*

open class CCCreateAnswerPlaceholderDialog(project: Project,
                                           placeholderText: String,
                                           isEdit: Boolean,
                                           private val placeholder: AnswerPlaceholder) : DialogWrapper(project, true) {
  private val panel: CCAddAnswerPlaceholderPanel = CCAddAnswerPlaceholderPanel(placeholderText)
  // "30" is the same value of text field columns as Messages.InputDialog uses
  val dependencyPathField = JBTextField(30)
  val visibilityCheckBox get() = JBCheckBox("Visible", placeholder.placeholderDependency?.isVisible == true)
  private val pathLabel = JLabel("sectionName#lessonName#taskName#[taskPath]#placeholderIndex")
  private val isFirstTask = placeholder.taskFile.task.isFirstInCourse
  private val currentText: String get() = dependencyPathField.text ?: ""
  private val taskText = StringUtil.notNullize(panel.getAnswerPlaceholderText()).trim { it <= ' ' }

  init {
    val title = (if (isEdit) "Edit" else "Add") + TITLE_SUFFIX
    this.title = title
    val buttonText = if (isEdit) "OK" else "Add"
    setOKButtonText(buttonText)
    this.init()
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
      contentPanel.border = JBUI.Borders.emptyBottom(15)
      val decorator = HideableDecorator(dependencyPanel, "Add Answer Placeholder Dependency", false)
      decorator.setContentComponent(contentPanel)
      panel.add(dependencyPanel, BorderLayout.CENTER)
    }
    return panel
  }

  public override fun doValidate(): ValidationInfo? {
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

  open fun getTaskText() = taskText

  private fun initialValue(placeholder: AnswerPlaceholder): String {
    // TODO: come up with better initial value
    return placeholder.placeholderDependency?.toString() ?: "lesson1#task1#path/task.txt#1"
  }

  companion object {
    private val TITLE_SUFFIX = " Answer Placeholder"
  }

  fun getDependencyInfo() = if (!(currentText.isBlank() || isFirstTask)) DependencyInfo(currentText, visibilityCheckBox.isSelected)
  else null

  data class DependencyInfo(val dependencyPath: String, val isVisible: Boolean)
}

  private val Task.isFirstInCourse: Boolean
    get() {
      if (index > 1) {
        return false
      }
      val section = lesson.section ?: return lesson.index == 1
      return section.index == 1 && lesson.index == 1
    }