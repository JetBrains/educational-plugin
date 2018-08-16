package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.PreviousTaskAction
import icons.EducationalCoreIcons

class TaskDescriptionToolWindowFactory : ToolWindowFactory, DumbAware {

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    if (!EduUtils.isStudyProject(project)) {
      return
    }
    toolWindow.icon = EducationalCoreIcons.CourseToolWindow
    val taskDescriptionToolWindow = if (EduUtils.hasJavaFx() && EduSettings.getInstance().shouldUseJavaFx()) {
      JavaFxToolWindow()
    }
    else {
      SwingToolWindow()
    }
    taskDescriptionToolWindow.init(project)
    toolWindow.initTitleActions()
    val contentManager = toolWindow.contentManager
    val content = contentManager.factory.createContent(taskDescriptionToolWindow, null, false)
    content.isCloseable = false
    contentManager.addContent(content)
    Disposer.register(project, taskDescriptionToolWindow)
  }

  private fun ToolWindow.initTitleActions() {
    val actions = arrayOf(PreviousTaskAction.ACTION_ID, NextTaskAction.ACTION_ID).map {
      ActionManager.getInstance().getAction(it) ?: error("Action $it not found")
    }.toTypedArray()
    (this as ToolWindowEx).setTitleActions(*actions)
  }

  companion object {
    const val STUDY_TOOL_WINDOW = "Task Description"
  }
}
