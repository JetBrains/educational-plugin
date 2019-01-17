package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.*
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.NewStudyItemUiModel
import javax.swing.JComponent

abstract class CCCreateStudyItemDialogBase(
  project: Project,
  protected val model: NewStudyItemUiModel,
  protected val additionalPanels: List<AdditionalPanel>
) : CCDialogWrapperBase(project) {

  private val nameField: JBTextField = JBTextField(model.suggestedName, 30)
  private val validator: InputValidatorEx = CCUtils.PathInputValidator(model.parentDir)

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
        else -> performCustomNameValidation(text!!) // text is not null here because of the first check
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
  protected open fun performCustomNameValidation(name: String): String? = null
}

class CCCreateStudyItemDialog(
  project: Project,
  model: NewStudyItemUiModel,
  additionalPanels: List<AdditionalPanel>
) : CCCreateStudyItemDialogBase(project, model, additionalPanels) {
  init { init() }
}
