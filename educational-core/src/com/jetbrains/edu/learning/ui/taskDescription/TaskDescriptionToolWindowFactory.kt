package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
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
    val contentManager = toolWindow.contentManager
    val content = contentManager.factory.createContent(taskDescriptionToolWindow, null, false)
    content.isCloseable = false
    contentManager.addContent(content)
    Disposer.register(project, taskDescriptionToolWindow)
  }

  companion object {
    const val STUDY_TOOL_WINDOW = "Task Description"
  }
}
