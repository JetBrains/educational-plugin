package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.*
import com.jetbrains.edu.coursecreator.CCStudyItemPathInputValidator
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.NewStudyItemUiModel
import com.jetbrains.edu.learning.courseFormat.Course
import javax.swing.JComponent

abstract class CCCreateStudyItemDialogBase(
  project: Project,
  course: Course,
  protected val model: NewStudyItemUiModel,
  protected val additionalPanels: List<AdditionalPanel>
) : CCDialogWrapperBase(project) {

  private val nameField: JBTextField = JBTextField(model.suggestedName, 30)
  private val validator: InputValidatorEx = CCStudyItemPathInputValidator(course, model.itemType, model.parentDir)

  protected val positionPanel: CCItemPositionPanel? = additionalPanels.find { it is CCItemPositionPanel } as? CCItemPositionPanel

  init {
    title = "Create New ${StringUtil.toTitleCase(model.itemType.presentableName)}"
  }

  override fun postponeValidation(): Boolean = false

  override fun createCenterPanel(): JComponent {
    addTextValidator(nameField) { text ->
      when {
        text.isNullOrEmpty() -> "Empty name"
        !validator.checkInput(text) -> validator.getErrorText(text)
        else -> null
      }
    }
    return panel {
      row("Name:") { nameField() }
      createAdditionalFields(this)
      additionalPanels.forEach { it.attach(this) }
    }
  }

  override fun getPreferredFocusedComponent(): JComponent? = nameField

  open fun showAndGetResult(): NewStudyItemInfo? =
    if (showAndGet()) NewStudyItemInfo(nameField.text, model.baseIndex + (positionPanel?.indexDelta ?: 0)) else null

  protected open fun createAdditionalFields(builder: LayoutBuilder) {}
}

class CCCreateStudyItemDialog(
  project: Project,
  course: Course,
  model: NewStudyItemUiModel,
  additionalPanels: List<AdditionalPanel>
) : CCCreateStudyItemDialogBase(project, course, model, additionalPanels) {
  init { init() }
}
