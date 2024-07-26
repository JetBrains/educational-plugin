package com.jetbrains.edu.csharp

import com.intellij.ide.ui.customization.CustomizationUtil
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.util.ui.tree.TreeUtil
import com.jetbrains.edu.learning.projectView.CourseViewPaneCustomization
import com.jetbrains.rider.projectView.views.impl.SolutionViewContextMenu
import javax.swing.JTree

class CSharpCourseViewPaneCustomization : CourseViewPaneCustomization {
  override fun customize(tree: JTree) {
    CustomizationUtil.installPopupHandler(tree, SolutionViewContextMenu.Id, ActionPlaces.PROJECT_VIEW_POPUP)
    TreeUtil.installActions(tree)
  }
}
