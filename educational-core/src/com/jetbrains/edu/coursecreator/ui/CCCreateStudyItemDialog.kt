package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.*
import com.jetbrains.edu.coursecreator.CCStudyItemPathInputValidator
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemUiModel
import com.jetbrains.edu.coursecreator.createItemTitleMessage
import com.jetbrains.edu.coursecreator.ui.CCItemPositionPanel.Companion.AFTER_DELTA
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

abstract class CCCreateStudyItemDialogBase(
  project: Project,
  course: Course,
  protected val model: NewStudyItemUiModel
) : CCDialogWrapperBase(project) {

  private val nameField: JBTextField = JBTextField(model.suggestedName, TEXT_FIELD_COLUMNS)
  private val validator: InputValidatorEx = CCStudyItemPathInputValidator(project, course, model.itemType, model.parentDir)


  init {
    title = model.itemType.createItemTitleMessage
  }

  override fun postponeValidation(): Boolean = false

  override fun createCenterPanel(): JComponent {
    addTextValidator(nameField) { it.getValidatorText() }
    return panel {
      if (showNameField()) {
        row("${EduCoreBundle.message("course.creator.new.study.item.label.name")}:") { nameField() }
      }
      createAdditionalFields(this)
    }
  }

  @Nls
  private fun String?.getValidatorText() =
    when {
      isNullOrEmpty() -> EduCoreBundle.message("course.creator.new.study.item.empty.name")
      !validator.checkInput(this) -> validator.getErrorText(this)
      else -> null
    }

  override fun getPreferredFocusedComponent(): JComponent? = nameField

  open fun showAndGetResult(): NewStudyItemInfo? =
    if (showAndGet()) createNewStudyItemInfo() else null

  protected open fun createNewStudyItemInfo(): NewStudyItemInfo {
    return NewStudyItemInfo(
      nameField.text,
      model.baseIndex + AFTER_DELTA,
      model.studyItemVariants.first().producer
    )
  }

  protected open fun createAdditionalFields(builder: LayoutBuilder) {}
  protected open fun showNameField(): Boolean = true

  companion object {
    const val TEXT_FIELD_COLUMNS: Int = 30
  }
}
