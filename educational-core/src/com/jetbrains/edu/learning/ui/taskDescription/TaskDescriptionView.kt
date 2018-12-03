package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.wm.ToolWindow
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.awt.Color
import java.awt.event.InputEvent

abstract class TaskDescriptionView {

  abstract var currentTask: Task?

  abstract fun init(toolWindow: ToolWindow)

  abstract fun updateTaskSpecificPanel()
  abstract fun updateTaskDescription(task: Task?)
  abstract fun updateTaskDescription()
  abstract fun updateAdditionalTaskTab()

  abstract fun readyToCheck()
  abstract fun checkStarted()
  abstract fun checkFinished(task: Task, checkResult: CheckResult)
  abstract fun showBalloon(text: String, messageType: MessageType, inputEvent: InputEvent?)

  companion object {

    @JvmStatic
    fun getInstance(project: Project): TaskDescriptionView {
      if (!EduUtils.isStudyProject(project)) {
        error("Attempt to get TaskDescriptionView for non-edu project")
      }
      return ServiceManager.getService(project, TaskDescriptionView::class.java)
    }

    @JvmStatic
    fun getTaskDescriptionBackgroundColor(): Color {
      return UIUtil.getListBackground()
    }
  }
}
