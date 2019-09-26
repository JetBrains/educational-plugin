package com.jetbrains.edu.cpp

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.NewStudyItemUiModel
import com.jetbrains.edu.coursecreator.ui.AdditionalPanel
import com.jetbrains.edu.coursecreator.ui.CCCreateStudyItemDialogBase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.configurator

class CppNewStudyItemDialog(
  val project: Project,
  model: NewStudyItemUiModel,
  additionalPanels: List<AdditionalPanel>
) : CCCreateStudyItemDialogBase(project, model, additionalPanels) {

  init {
    init()
  }

  override fun performCustomNameValidation(name: String): String? =
    project.course?.configurator?.isValidItemName(name)
}