package com.jetbrains.edu.learning.taskToolWindow.links

import com.intellij.facet.ui.FacetDependentToolWindow
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowEP
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ext.LibraryDependentToolWindow

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
    val hasToolWindow = TOOL_WINDOW_EPS.flatMap { it.extensionList }.any { it.id == toolWindowId }
    return if (hasToolWindow) null else "Failed to find a tool window by `$toolWindowId` id"
  }

  companion object {
    private val TOOL_WINDOW_EPS = listOf(
      ToolWindowEP.EP_NAME,
      LibraryDependentToolWindow.EXTENSION_POINT_NAME,
      FacetDependentToolWindow.EXTENSION_POINT_NAME
    )
  }
}
