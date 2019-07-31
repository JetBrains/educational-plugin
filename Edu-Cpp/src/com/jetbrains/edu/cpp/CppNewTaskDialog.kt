package com.jetbrains.edu.cpp

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.jetbrains.edu.coursecreator.actions.NewStudyItemUiModel
import com.jetbrains.edu.coursecreator.ui.AdditionalPanel
import com.jetbrains.edu.coursecreator.ui.CCCreateStudyItemDialogBase

class CppNewTaskDialog(
  project: Project,
  model: NewStudyItemUiModel,
  additionalPanels: List<AdditionalPanel>
) : CCCreateStudyItemDialogBase(project, model, additionalPanels) {
  private val namePattern: Regex = "[-a-zA-Z0-9_]*".toRegex()

  init {
    init()
  }

  override fun performCustomNameValidation(name: String): String? =
    if (name.matches(namePattern)) null else "PANIC"


  override fun doValidateAll(): List<ValidationInfo> {
    val validationInfos = ArrayList(super.doValidateAll())

    val validationInfo = validationInfos.find { it.message == "PANIC" }
    validationInfos.remove(validationInfo)
    validationInfo?.let {
      validationInfos += ValidationInfo("Use pattern '${namePattern}' for the name to omit problems with CMake",
                                        it.component).asWarning().withOKEnabled()
    }
    return validationInfos
  }
}