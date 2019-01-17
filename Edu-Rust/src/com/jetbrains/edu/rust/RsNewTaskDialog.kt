package com.jetbrains.edu.rust

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.NewStudyItemUiModel
import com.jetbrains.edu.coursecreator.ui.AdditionalPanel
import com.jetbrains.edu.coursecreator.ui.CCCreateStudyItemDialogBase
import org.rust.ide.newProject.RsPackageNameValidator

class RsNewTaskDialog(
  project: Project,
  model: NewStudyItemUiModel,
  additionalPanels: List<AdditionalPanel>
) : CCCreateStudyItemDialogBase(project, model, additionalPanels) {

  init { init() }

  override fun performCustomNameValidation(name: String): String? = RsPackageNameValidator.validate(name.toPackageName(), true)
}
