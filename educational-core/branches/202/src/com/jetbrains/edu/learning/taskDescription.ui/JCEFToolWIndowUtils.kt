package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import io.netty.handler.codec.http.HttpRequest
import javax.swing.JComponent
import com.intellij.util.io.getHostName as nettyGetHostName

fun isSupported(): Boolean = JBCefApp.isSupported()

fun getJCEFToolWindow(project: Project): TaskDescriptionToolWindow? = JCEFToolWindow(project)

fun getJCEFComponent(html: String): JComponent? {
  val browser = JBCefBrowser()
  browser.loadHTML(html)
  return browser.component
}

fun getHostName(httpRequest: HttpRequest): String? = nettyGetHostName(httpRequest)