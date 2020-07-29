package com.jetbrains.edu.coursecreator.actions.create

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemUiModel
import com.jetbrains.edu.coursecreator.ui.CCItemPositionPanel.Companion.AFTER_DELTA
import com.jetbrains.edu.coursecreator.ui.NewStudyItemInfoCallback
import com.jetbrains.edu.coursecreator.ui.NewStudyItemUi
import com.jetbrains.edu.learning.courseFormat.Course

open class MockNewStudyItemUi(
  private val name: String? = null,
  private val index: Int? = null,
  private val itemType: String? = null
): NewStudyItemUi {

  var errorMessage: String? = null

  override fun show(
    project: Project,
    course: Course,
    model: NewStudyItemUiModel,
    callback: NewStudyItemInfoCallback
  ) {
    val itemVariant = if (itemType != null) {
      model.studyItemVariants.find { it.type == itemType } ?: error("Can't find `$itemType` in `${model.studyItemVariants.map { it.type }}`")
    }
    else {
      model.studyItemVariants.first()
    }

    val info = NewStudyItemInfo(
      name ?: model.suggestedName,
      index ?: model.baseIndex + AFTER_DELTA,
      itemVariant.producer
    )
    callback(info) {
      errorMessage = it
    }
  }
}
