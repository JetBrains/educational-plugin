package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.NewStudyItemUiModel
import com.jetbrains.edu.learning.courseFormat.Course

fun createNewStudyItemUi(
  dialogGenerator: (Project, Course, NewStudyItemUiModel, List<AdditionalPanel>) -> CCCreateStudyItemDialogBase
): NewStudyItemUi {
  return NewStudyItemDialogUi(dialogGenerator)
}
