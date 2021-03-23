package com.jetbrains.edu.learning.taskDescription.ui.tab

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JCEFHtmlPanel
import com.jetbrains.edu.learning.taskDescription.ui.*
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabManager.TabType
import org.jsoup.nodes.Element
import java.awt.BorderLayout

class JCEFTabPanel(project: Project, tabType: TabType) : TabPanel(project, tabType) {
  private val jcefBrowser = JCEFHtmlPanel(JBCefApp.getInstance().createClient(), null)
  private val jcefLinkHandler = JCefToolWindowLinkHandler(project)
  private val taskInfoRequestHandler = ToolWindowRequestHandler(jcefLinkHandler)
  private val taskInfoLifeSpanHandler = TaskInfoLifeSpanHandler(jcefLinkHandler)

  init {
    // BACKCOMPAT: 2020.3: error page is disabled by default in 211 branch
    jcefBrowser.disableErrorPage()
    jcefBrowser.jbCefClient.addRequestHandler(taskInfoRequestHandler, jcefBrowser.cefBrowser)
    jcefBrowser.jbCefClient.addLifeSpanHandler(taskInfoLifeSpanHandler, jcefBrowser.cefBrowser)
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

  override fun wrapHint(hintElement: Element, displayedHintNumber: String): String {
    return JCEFToolWindow.wrapHint(project, hintElement, displayedHintNumber)
  }
}