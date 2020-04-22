package com.jetbrains.edu.learning.taskDescription.ui

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

  override fun wrapHint(hintText: Element, displayedHintNumber: String): String {
    TODO("Not yet implemented")
  }

  override fun setText(text: String, task: Task?) {
    val html = htmlWithResources(project, wrapHints(text, task))
    taskInfoJBCefBrowser.loadHTML(html)
  }

  override fun updateLaf() {}
}