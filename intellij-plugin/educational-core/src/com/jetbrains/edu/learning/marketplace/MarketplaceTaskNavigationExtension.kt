package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.actions.ShareMySolutionsAction
import com.jetbrains.edu.learning.navigation.TaskNavigationExtension
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView

class MarketplaceTaskNavigationExtension : TaskNavigationExtension {

  override fun onTaskNavigation(project: Project, task: Task, fromTask: Task?) {
    if (!project.isMarketplaceCourse()) return

    if (Registry.`is`(ShareMySolutionsAction.REGISTRY_KEY, false)) {
      TaskToolWindowView.getInstance(project).showMyTab()
    }
  }
}