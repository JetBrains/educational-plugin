package com.jetbrains.edu.cpp

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.NewStudyItemUiModel
import com.jetbrains.edu.coursecreator.ui.AdditionalPanel
import com.jetbrains.edu.coursecreator.ui.CCCreateStudyItemDialogBase

class CppNewStudyItemDialog(
  project: Project,
  model: NewStudyItemUiModel,
  additionalPanels: List<AdditionalPanel>
) : CCCreateStudyItemDialogBase(project, model, additionalPanels) {

  init {
    init()
  }

  override fun performCustomNameValidation(name: String): String? = validateStudyItemName(name)
}