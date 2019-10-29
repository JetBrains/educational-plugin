package com.jetbrains.edu.coursecreator.actions.create

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.NewStudyItemUiModel
import com.jetbrains.edu.coursecreator.ui.AdditionalPanel
import com.jetbrains.edu.coursecreator.ui.NewStudyItemUi
import com.jetbrains.edu.learning.courseFormat.Course

open class MockNewStudyItemUi(private val name: String? = null, private val index: Int? = null): NewStudyItemUi {
  override fun showDialog(
    project: Project,
    course: Course,
    model: NewStudyItemUiModel,
    additionalPanels: List<AdditionalPanel>
  ): NewStudyItemInfo? =
    NewStudyItemInfo(name ?: model.suggestedName, index ?: model.baseIndex)
}
