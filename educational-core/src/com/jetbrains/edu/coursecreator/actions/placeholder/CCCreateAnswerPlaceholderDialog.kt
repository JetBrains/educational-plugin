package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.jetbrains.edu.coursecreator.CCUtils.DEFAULT_PLACEHOLDER_TEXT
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.messages.EduCoreBundle
import javax.swing.JComponent

open class CCCreateAnswerPlaceholderDialog(
  project: Project,
  placeholder: AnswerPlaceholder
) : DialogWrapper(project, true) {

  private val panel: CCAddAnswerPlaceholderPanel = CCAddAnswerPlaceholderPanel(placeholder.placeholderText ?: DEFAULT_PLACEHOLDER_TEXT)

  init {
    title = EduCoreBundle.message("ui.dialog.create.answer.placeholder.add")
    val buttonText = EduCoreBundle.message("label.add")
    setOKButtonText(buttonText)
    super.init()
    initValidation()
  }

  override fun createCenterPanel(): JComponent {
    return panel
  }

  override fun doValidate(): ValidationInfo? {
    return null
  }

  override fun getPreferredFocusedComponent(): JComponent? {
    return panel.getPreferredFocusedComponent()
  }

  open fun getPlaceholderText(): String = panel.getAnswerPlaceholderText().trim()

}
