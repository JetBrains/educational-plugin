package com.jetbrains.edu.ai.terms.ui

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.navigation.TaskNavigationExtension

class TermsTaskNavigationExtension : TaskNavigationExtension {
  override fun onTaskNavigation(project: Project, task: Task, fromTask: Task?) {
    TermsGotItTooltipService.getInstance(project).showTermsGotItTooltip(task)
  }
}