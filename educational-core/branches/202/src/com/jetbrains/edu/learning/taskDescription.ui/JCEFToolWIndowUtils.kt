package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import javax.swing.JComponent

fun isSupported(): Boolean = JBCefApp.isSupported()

fun getJCEFToolWindow(project: Project): TaskDescriptionToolWindow? = JCEFToolWindow(project)

fun getJCEFComponent(html: String): JComponent? {
  val browser = JBCefBrowser()
  browser.loadHTML(html)
  return browser.component
}
