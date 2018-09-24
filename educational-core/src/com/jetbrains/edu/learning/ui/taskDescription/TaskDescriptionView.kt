package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.awt.Color

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

    @JvmStatic
    fun getTaskDescriptionBackgroundColor(): Color {
      return if (!UIUtil.isUnderDarcula()) {
        EditorColorsManager.getInstance().globalScheme.defaultBackground
      }
      else Color(0x3c3f41)
    }
  }
}
