package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefBrowser
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import javax.swing.JComponent

class JCEFToolWindow(project: Project) : TaskDescriptionToolWindow(project) {
  private val taskInfoJBCefBrowser = JBCefBrowser()
  private val taskSpecificJBCefBrowser = JBCefBrowser()

  override fun createTaskInfoPanel(): JComponent {
    return taskInfoJBCefBrowser.component
  }

  override fun createTaskSpecificPanel(): JComponent {
    // TODO Later
    return taskSpecificJBCefBrowser.component
  }

  override fun setText(text: String, task: Task?) {
    val html = htmlWithResources(project, wrapHints(text, task))
    taskInfoJBCefBrowser.loadHTML(html)
  }

  override fun updateLaf() {}
}