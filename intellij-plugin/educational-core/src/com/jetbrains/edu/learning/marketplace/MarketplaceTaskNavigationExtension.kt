package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.navigation.TaskNavigationExtension
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView

class MarketplaceTaskNavigationExtension : TaskNavigationExtension {

  override fun onTaskNavigation(project: Project, task: Task, fromTask: Task?) {
    if (!project.isMarketplaceCourse()) return
    TaskToolWindowView.getInstance(project).showMyTab()
  }
}