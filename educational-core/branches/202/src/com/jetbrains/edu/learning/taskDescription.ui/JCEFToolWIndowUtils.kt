package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefBrowser
import javax.swing.JComponent

fun getJCEFToolWindow(project: Project): TaskDescriptionToolWindow? = JCEFToolWindow(project)

fun getJCEFComponent(html: String): JComponent? {
  val browser = JBCefBrowser()
  browser.loadHTML(html)
  return browser.component
}
