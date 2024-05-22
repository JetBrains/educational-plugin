package com.jetbrains.edu.learning.taskToolWindow.ui.jcef

import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JCEFHtmlPanel
import com.jetbrains.edu.learning.taskToolWindow.links.JCefToolWindowLinkHandler
import com.jetbrains.edu.learning.taskToolWindow.ui.JCEFTaskInfoLifeSpanHandler
import com.jetbrains.edu.learning.taskToolWindow.ui.JCEFToolWindowRequestHandler

class TaskInfoJBCefBrowser(project: Project) : JCEFHtmlPanel(true, JBCefApp.getInstance().createClient(), null) {

  init {
    val jcefLinkInToolWindowHandler = JCefToolWindowLinkHandler(project)
    val taskInfoRequestHandler = JCEFToolWindowRequestHandler(jcefLinkInToolWindowHandler)
    jbCefClient.addRequestHandler(taskInfoRequestHandler, cefBrowser)

    jbCefClient.addLifeSpanHandler(JCEFTaskInfoLifeSpanHandler(jcefLinkInToolWindowHandler), cefBrowser)
  }
}