package com.jetbrains.edu.coursecreator.actions.create

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.NewStudyItemUiModel
import com.jetbrains.edu.coursecreator.ui.NewStudyItemUi
import com.jetbrains.edu.coursecreator.ui.CCItemPositionPanel

class MockNewStudyItemUi(private val name: String, private val index: Int? = null): NewStudyItemUi {
  override fun showDialog(project: Project, model: NewStudyItemUiModel, positionPanel: CCItemPositionPanel?): NewStudyItemInfo? =
    NewStudyItemInfo(name, index ?: model.baseIndex)
}
