package com.jetbrains.edu.learning.taskToolWindow.links

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class ToolWindowLink(link: String) : TaskDescriptionLink<ToolWindow>(link) {
  override fun resolve(project: Project): ToolWindow? {
    return ToolWindowManager.getInstance(project).getToolWindow(linkPath)
  }

  override fun open(project: Project, toolWindow: ToolWindow) {
    runInEdt {
      toolWindow.show()
    }
  }
}
