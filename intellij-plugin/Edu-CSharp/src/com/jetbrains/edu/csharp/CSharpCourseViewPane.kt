package com.jetbrains.edu.csharp

import com.intellij.ide.ui.customization.CustomizationUtil
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.project.Project
import com.intellij.util.ui.tree.TreeUtil
import com.jetbrains.edu.learning.projectView.CourseViewPane
import com.jetbrains.rider.projectView.views.impl.SolutionViewContextMenu
import javax.swing.JComponent

class CSharpCourseViewPane(project: Project) : CourseViewPane(project) {
  override fun createComponent(): JComponent {
    val result = super.createComponent()
    CustomizationUtil.installPopupHandler(myTree, SolutionViewContextMenu.Id, ActionPlaces.PROJECT_VIEW_POPUP)
    TreeUtil.installActions(myTree)
    return result
  }
}