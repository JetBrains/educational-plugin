package com.jetbrains.edu.learning.checker.details

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.edu.learning.EduUtils

class CheckDetailsView(val project: Project) {

  companion object {
    @JvmStatic
    fun getInstance(project: Project): CheckDetailsView {
      if (!EduUtils.isStudyProject(project)) {
        error("Attempt to get CheckDetailsView for non-edu project")
      }
      return ServiceManager.getService(project, CheckDetailsView::class.java)
    }
  }

  fun showOutput(message: String) {
    printToConsole("Output", message, ConsoleViewContentType.NORMAL_OUTPUT)
  }

  fun showCompilationResults(message: String) {
    printToConsole("Compilation Results", message, ConsoleViewContentType.ERROR_OUTPUT)
  }

  private fun getToolWindow(): ToolWindow {
    return ToolWindowManager.getInstance(project).getToolWindow(CheckDetailsToolWindowFactory.ID)
           ?: error("CheckDetails tool window not found")
  }

  private fun printToConsole(title: String, message: String, contentType: ConsoleViewContentType) {
    val toolWindow = getToolWindow()
    runInEdt {
      val contentManager = toolWindow.contentManager
      contentManager.removeAllContents(true)
      val consoleView = ConsoleViewImpl(project, true)
      val content = contentManager.factory.createContent(consoleView.component, title, false)
      contentManager.addContent(content)
      consoleView.print(message, contentType)
      toolWindow.setAvailable(true, null)
      toolWindow.show(null)
    }
  }
}