@file:JvmName("NewStudyItemUiUtils")

package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.NewStudyItemUiModel
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.isUnitTestMode
import org.jetbrains.annotations.TestOnly

private var MOCK: NewStudyItemUi? = null

@JvmOverloads
fun showNewStudyItemDialog(
  project: Project,
  course: Course,
  model: NewStudyItemUiModel,
  additionalPanels: List<AdditionalPanel>,
  dialogGenerator: (Project, Course, NewStudyItemUiModel, List<AdditionalPanel>) -> CCCreateStudyItemDialogBase = ::CCCreateStudyItemDialog,
  studyItemCreator: (NewStudyItemInfo) -> Unit
) {
  val ui = if (isUnitTestMode) {
    MOCK ?: error("You should set mock ui via `withMockCreateStudyItemUi`")
  } else {
    createNewStudyItemUi(dialogGenerator)
  }
  ui.show(project, course, model, additionalPanels, studyItemCreator)
}

@TestOnly
fun withMockCreateStudyItemUi(mockUi: NewStudyItemUi, action: () -> Unit) {
  try {
    MOCK = mockUi
    action()
  } finally {
    MOCK = null
  }
}

interface NewStudyItemUi {
  fun show(
    project: Project,
    course: Course,
    model: NewStudyItemUiModel,
    additionalPanels: List<AdditionalPanel>,
    studyItemCreator: (NewStudyItemInfo) -> Unit
  )
}

class NewStudyItemDialogUi(
  private val dialogGenerator: (Project, Course, NewStudyItemUiModel, List<AdditionalPanel>) -> CCCreateStudyItemDialogBase
) : NewStudyItemUi {
  override fun show(
    project: Project,
    course: Course,
    model: NewStudyItemUiModel,
    additionalPanels: List<AdditionalPanel>,
    studyItemCreator: (NewStudyItemInfo) -> Unit
  ) {
    val studyItemInfo = dialogGenerator(project, course, model, additionalPanels).showAndGetResult() ?: return
    studyItemCreator(studyItemInfo)
  }
}
