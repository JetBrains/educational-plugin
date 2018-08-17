package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task


class TaskDescriptionCheckListener: CheckListener {
  override fun beforeCheck(project: Project, task: Task) {
    TaskDescriptionPanel.getToolWindow(project)!!.icon.isVisible = true
  }

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    TaskDescriptionPanel.getToolWindow(project)!!.icon.isVisible = false
  }
}