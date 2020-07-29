@file:JvmName("NewStudyItemUiUtils")

package com.jetbrains.edu.coursecreator.ui

import com.intellij.ide.ui.newItemPopup.NewItemPopupUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.popup.JBPopup
import com.jetbrains.edu.coursecreator.CCStudyItemPathInputValidator
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemUiModel
import com.jetbrains.edu.coursecreator.newItemTitleMessage
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.isUnitTestMode
import org.jetbrains.annotations.TestOnly

private var MOCK: NewStudyItemUi? = null

fun showNewStudyItemDialog(
  project: Project,
  course: Course,
  model: NewStudyItemUiModel,
  studyItemCreator: (NewStudyItemInfo) -> Unit
) {
  val ui = if (isUnitTestMode) {
    MOCK ?: error("You should set mock ui via `withMockCreateStudyItemUi`")
  } else {
    NewStudyItemPopupUi()
  }
  val validator = CCStudyItemPathInputValidator(project, course, model.itemType, model.parentDir)
  val callback = NewStudyItemInfoCallback(validator, studyItemCreator)
  ui.show(project, course, model, callback)
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
    callback: NewStudyItemInfoCallback
  )
}

class NewStudyItemInfoCallback(
  val validator: InputValidatorEx,
  val studyItemCreator: (NewStudyItemInfo) -> Unit
) {
  operator fun invoke(info: NewStudyItemInfo, onActionCallback: (String?) -> Unit) {
    val name = info.name
    if (validator.checkInput(name) && validator.canClose(name)) {
      onActionCallback(null)
      studyItemCreator(info)
    }
    else {
      val errorMessage = validator.getErrorText(name)
      onActionCallback(errorMessage)
    }
  }
}

class NewStudyItemPopupUi : NewStudyItemUi {
  override fun show(
    project: Project,
    course: Course,
    model: NewStudyItemUiModel,
    callback: NewStudyItemInfoCallback
  ) {
    val popup = createLightWeightPopup(model, callback)
    popup.showCenteredInCurrentWindow(project)
  }

  private fun createLightWeightPopup(model: NewStudyItemUiModel, callback: NewStudyItemInfoCallback): JBPopup {
    val contentPanel = NewStudyItemPopupPanel(model.itemType, model.studyItemVariants)
    val nameField = contentPanel.textField
    nameField.text = model.suggestedName
    nameField.selectAll()
    val title = model.itemType.newItemTitleMessage
    val popup = NewItemPopupUtil.createNewItemPopup(title, contentPanel, nameField)
    contentPanel.setApplyAction { event ->
      val variant = contentPanel.getSelectedItem() ?: return@setApplyAction
      val info = NewStudyItemInfo(nameField.text, model.baseIndex + CCItemPositionPanel.AFTER_DELTA, variant.producer)
      callback(info) { errorMessage ->
        if (errorMessage == null) {
          popup.closeOk(event)
        }
        else {
          contentPanel.setError(errorMessage)
        }
      }
    }

    return popup
  }
}
