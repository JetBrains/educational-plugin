package com.jetbrains.edu.learning.taskToolWindow.links

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.edu.learning.collectToolWindowExtensions

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class ToolWindowLink(link: String) : TaskDescriptionLink<String, String>(link) {
  override fun resolve(project: Project): String = linkPath

  override fun open(project: Project, toolWindowId: String) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(linkPath) ?: return
    runInEdt {
      toolWindow.show()
    }
  }

  override suspend fun validate(project: Project, toolWindowId: String): String? {
    val hasToolWindow = collectToolWindowExtensions().any { it.id == toolWindowId }
    return if (hasToolWindow) null else "Failed to find a tool window by `$toolWindowId` id"
  }
}
