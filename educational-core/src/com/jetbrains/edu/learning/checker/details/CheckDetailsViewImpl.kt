package com.jetbrains.edu.learning.checker.details

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import javafx.embed.swing.JFXPanel
import javax.swing.JComponent

class CheckDetailsViewImpl(val project: Project) : CheckDetailsView() {

  override fun showOutput(message: String) {
    printToConsole("Output", message, ConsoleViewContentType.NORMAL_OUTPUT)
  }

  override fun showCheckResultDetails(title: String, message: String) = printToConsole(title, message, ConsoleViewContentType.ERROR_OUTPUT)

  private fun getToolWindow(): ToolWindow {
    return ToolWindowManager.getInstance(project).getToolWindow(CheckDetailsToolWindowFactory.ID)
           ?: error("CheckDetails tool window not found")
  }

  private fun createContentAndShowToolWindow(component: JComponent, title: String) {
    val toolWindow = getToolWindow()
    val contentManager = toolWindow.contentManager
    contentManager.removeAllContents(true)
    val content = contentManager.factory.createContent(component, title, false)
    contentManager.addContent(content)
    toolWindow.setAvailable(true, null)
    toolWindow.show(null)
  }

  private fun printToConsole(title: String, message: String, contentType: ConsoleViewContentType) {
    val consoleView = ConsoleViewImpl(project, true)
    consoleView.print(message, contentType)
    runInEdt {
      createContentAndShowToolWindow(consoleView.component, title)
    }
  }

  override fun showJavaFXResult(title: String, panel: JFXPanel) {
    runInEdt {
      createContentAndShowToolWindow(panel, title)
    }
  }

  override fun clear() {
    val toolWindow = getToolWindow()
    runInEdt {
      toolWindow.contentManager.removeAllContents(true)
    }
  }
}