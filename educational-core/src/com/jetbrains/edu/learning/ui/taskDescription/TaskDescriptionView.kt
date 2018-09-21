package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task

abstract class TaskDescriptionView : SimpleToolWindowPanel(true, true), Disposable {

  abstract var currentTask: Task?

  abstract fun init()

  abstract fun updateTaskSpecificPanel()
  abstract fun updateTaskDescription(task: Task?)
  abstract fun updateTaskDescription()

  abstract fun readyToCheck()
  abstract fun checkStarted()
  abstract fun checkFinished(checkResult: CheckResult)

  companion object {

    @JvmStatic
    fun getInstance(project: Project): TaskDescriptionView {
      if (!EduUtils.isStudyProject(project)) {
        error("Attempt to get TaskDescriptionView for non-edu project")
      }
      return ServiceManager.getService(project, TaskDescriptionView::class.java)
    }
  }
}
