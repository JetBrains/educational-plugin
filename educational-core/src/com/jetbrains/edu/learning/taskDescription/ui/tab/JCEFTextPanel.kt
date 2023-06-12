package com.jetbrains.edu.learning.taskDescription.ui.tab

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JCEFHtmlPanel
import com.jetbrains.edu.learning.taskDescription.ui.JCEFTaskInfoLifeSpanHandler
import com.jetbrains.edu.learning.taskDescription.ui.JCEFToolWindowRequestHandler
import com.jetbrains.edu.learning.taskDescription.ui.JCefToolWindowLinkHandler
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import java.awt.BorderLayout
import javax.swing.JComponent


class JCEFTextPanel(project: Project) : TabTextPanel(project) {
  private val jcefBrowser = JCEFHtmlPanel(true, JBCefApp.getInstance().createClient(), null)

  override val component: JComponent
    get() = jcefBrowser.component

  init {
    val toolWindowLinkHandler = JCefToolWindowLinkHandler(project)
    val requestHandler = JCEFToolWindowRequestHandler(toolWindowLinkHandler)
    jcefBrowser.jbCefClient.addRequestHandler(requestHandler, jcefBrowser.cefBrowser)
    val lifeSpanHandler = JCEFTaskInfoLifeSpanHandler(toolWindowLinkHandler)
    jcefBrowser.jbCefClient.addLifeSpanHandler(lifeSpanHandler, jcefBrowser.cefBrowser)
    add(jcefBrowser.component, BorderLayout.CENTER)

    Disposer.register(this, jcefBrowser)
    ApplicationManager.getApplication().messageBus.connect(this)
      .subscribe(LafManagerListener.TOPIC,
        LafManagerListener {
          TaskDescriptionView.updateAllTabs(project)
        })
  }

  override fun setText(text: String) {
    jcefBrowser.loadHTML(text)
  }
}