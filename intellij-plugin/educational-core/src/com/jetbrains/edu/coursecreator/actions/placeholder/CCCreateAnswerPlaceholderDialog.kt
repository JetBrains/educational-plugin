package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapperPeer
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.HideableDecorator
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.coursecreator.actions.placeholder.CCAddAnswerPlaceholderPanel.Companion.PLACEHOLDER_PANEL_WIDTH
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JPanel

open class CCCreateAnswerPlaceholderDialog(
  project: Project,
  isEdit: Boolean,
  private val placeholder: AnswerPlaceholder
) : DialogWrapper(project, true) {

  private val panel: CCAddAnswerPlaceholderPanel = CCAddAnswerPlaceholderPanel(placeholder)
  private val dependencyPathField: JBTextField = JBTextField(0)
  private val isFirstTask: Boolean = placeholder.taskFile.task.isFirstInCourse
  private val currentText: String get() = dependencyPathField.text ?: ""

  init {
    title =
      if (isEdit) EduCoreBundle.message("ui.dialog.create.answer.placeholder.edit")
      else EduCoreBundle.message("ui.dialog.create.answer.placeholder.add")
    val buttonText = if (isEdit) EduCoreBundle.message("label.ok") else EduCoreBundle.message("label.add")
    setOKButtonText(buttonText)
    super.init()
    initValidation()
  }

  override fun createCenterPanel(): JComponent {
    dependencyPathField.putClientProperty(DialogWrapperPeer.HAVE_INITIAL_SELECTION, true)

    if (!isFirstTask) {
      val dependencyPanel = JPanel(BorderLayout())
      val contentPanel = panel {
        row {
          cell(dependencyPathField)
            .align(AlignX.FILL)
        }
        row {
          comment(EduCoreBundle.message("ui.dialog.create.answer.placeholder.path.pattern"))
        }
      }
      contentPanel.border = JBUI.Borders.emptyBottom(5)
      val decorator = HideableDecorator(dependencyPanel, EduCoreBundle.message("ui.dialog.create.answer.placeholder.dependency"), true)
      decorator.setContentComponent(contentPanel)
      if (placeholder.placeholderDependency != null) {
        decorator.setOn(true)
        dependencyPathField.text = placeholder.placeholderDependency?.toString()
        panel.preferredSize = JBUI.size(PLACEHOLDER_PANEL_WIDTH, 330)
      }

      dependencyPanel.alignmentX = Component.LEFT_ALIGNMENT
      contentPanel.maximumSize = JBUI.size(Int.MAX_VALUE, 0)
      dependencyPathField.minimumSize = JBUI.size(PLACEHOLDER_PANEL_WIDTH, 0)
      panel.add(dependencyPanel, BorderLayout.SOUTH)
      panel.minimumSize = JBUI.size(PLACEHOLDER_PANEL_WIDTH, 180)
    }
    return panel
  }

  override fun doValidate(): ValidationInfo? {
    if (currentText.isEmpty()) {
      return null
    }
    val errorText = try {
      val dependency = AnswerPlaceholderDependency.create(placeholder, currentText)
      if (dependency == null) EduCoreBundle.message("error.invalid.dependency") else null
    }
    catch (e: AnswerPlaceholderDependency.InvalidDependencyException) {
      e.customMessage
    }
    return if (errorText != null) ValidationInfo(errorText) else null
  }

  override fun getPreferredFocusedComponent(): JComponent? {
    return panel.getPreferredFocusedComponent()
  }

  open fun getPlaceholderText(): String = panel.getAnswerPlaceholderText().trim()

  open fun getVisible(): Boolean = panel.getVisible()

  open fun getDependencyInfo(): DependencyInfo? =
    if (!(currentText.isBlank() || isFirstTask)) {
      DependencyInfo(currentText)
    }
    else null

  data class DependencyInfo(val dependencyPath: String)
}

private val Task.isFirstInCourse: Boolean
  get() {
    if (index > 1) {
      return false
    }
    val section = lesson.section ?: return lesson.index == 1
    return section.index == 1 && lesson.index == 1
  }