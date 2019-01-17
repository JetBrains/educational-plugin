@file:JvmName("NewStudyItemUiUtils")

package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.NewStudyItemUiModel
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.learning.isUnitTestMode
import org.jetbrains.annotations.TestOnly

private var MOCK: NewStudyItemUi? = null

@JvmOverloads
fun showNewStudyItemDialog(
  project: Project,
  model: NewStudyItemUiModel,
  additionalPanels: List<AdditionalPanel>,
  dialogGenerator: (Project, NewStudyItemUiModel, List<AdditionalPanel>) -> CCCreateStudyItemDialogBase = ::CCCreateStudyItemDialog
): NewStudyItemInfo? {
  val ui = if (isUnitTestMode) {
    MOCK ?: error("You should set mock ui via `withMockCreateStudyItemUi`")
  } else {
    NewStudyItemDialogUi(dialogGenerator)
  }
  return ui.showDialog(project, model, additionalPanels)
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
  fun showDialog(project: Project, model: NewStudyItemUiModel, additionalPanels: List<AdditionalPanel>): NewStudyItemInfo?
}

class NewStudyItemDialogUi(
  private val dialogGenerator: (Project, NewStudyItemUiModel, List<AdditionalPanel>) -> CCCreateStudyItemDialogBase
) : NewStudyItemUi {
  override fun showDialog(
    project: Project,
    model: NewStudyItemUiModel,
    additionalPanels: List<AdditionalPanel>
  ): NewStudyItemInfo? = dialogGenerator(project, model, additionalPanels).showAndGetResult()
}
