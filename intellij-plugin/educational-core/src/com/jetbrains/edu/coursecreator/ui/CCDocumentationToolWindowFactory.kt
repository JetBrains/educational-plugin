package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject

class CCDocumentationToolWindowFactory : ToolWindowFactory, DumbAware {
  override suspend fun isApplicableAsync(project: Project) = project.isEduProject()

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    MarkdownPan
    val contentFactory = ContentFactory.getInstance()
    val content = contentFactory.createContent(myToolWindow.getContent(), "", false)
    toolWindow.contentManager.addContent(content)
  }

  companion object {
    const val ID = "Docs"
  }



}

private class DocumentationToolWindowView : ToolWindowView {

}