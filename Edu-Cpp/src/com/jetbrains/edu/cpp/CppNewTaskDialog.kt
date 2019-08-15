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
  private val namePattern: Regex = "[a-zA-Z0-9_]+".toRegex()

  init {
    init()
  }

  override fun performCustomNameValidation(name: String): String? =
    if (name.matches(namePattern)) null else "Name should contain only latin letters, digits or '_' symbols."
}