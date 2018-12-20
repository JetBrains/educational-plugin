package com.jetbrains.edu.rust

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.NewStudyItemUiModel
import com.jetbrains.edu.coursecreator.ui.CCCreateStudyItemDialogBase
import com.jetbrains.edu.coursecreator.ui.CCItemPositionPanel
import org.rust.ide.newProject.RsPackageNameValidator

class RsNewTaskDialog(
  project: Project,
  model: NewStudyItemUiModel,
  positionPanel: CCItemPositionPanel?
) : CCCreateStudyItemDialogBase(project, model, positionPanel) {

  init { init() }

  override fun performCustomNameValidation(name: String): String? = RsPackageNameValidator.validate(name.toPackageName(), false)
}
