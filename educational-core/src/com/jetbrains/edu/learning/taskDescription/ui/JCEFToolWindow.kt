package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JCEFHtmlPanel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.cef.handler.CefLoadHandlerAdapter
import org.jetbrains.annotations.TestOnly
import org.jsoup.nodes.Element
import javax.swing.JComponent

class JCEFToolWindow(project: Project) : TaskDescriptionToolWindow(project) {
  private val taskSpecificJBCefBrowser = JCEFHtmlPanel(true, JBCefApp.getInstance().createClient(), null)

  private var taskSpecificLoadHandler: CefLoadHandlerAdapter? = null

  init {
    Disposer.register(this, taskSpecificJBCefBrowser)

    ApplicationManager.getApplication().messageBus.connect(this)
      .subscribe(LafManagerListener.TOPIC,
                 LafManagerListener { TaskDescriptionView.updateAllTabs(project) })
  }

  override val taskSpecificPanel: JComponent
    get() = taskSpecificJBCefBrowser.component

  override fun wrapHint(hintElement: Element, displayedHintNumber: String, hintTitle: String): String {
    return wrapHintJCEF(project, hintElement, displayedHintNumber, hintTitle)
  }

  override fun updateTaskSpecificPanel(task: Task?) {
    taskSpecificJBCefBrowser.component.isVisible = false

    val taskText = getHTMLTemplateText(task) ?: return

    val newLoadHandler = getTaskSpecificQueryManager(task, taskSpecificJBCefBrowser)?.getTaskSpecificLoadHandler() ?: return

    taskSpecificLoadHandler?.let {
      taskSpecificJBCefBrowser.jbCefClient.removeLoadHandler(it, taskSpecificJBCefBrowser.cefBrowser)
    }

    taskSpecificJBCefBrowser.jbCefClient.addLoadHandler(newLoadHandler, taskSpecificJBCefBrowser.cefBrowser)
    taskSpecificLoadHandler = newLoadHandler

    taskSpecificJBCefBrowser.component.preferredSize = JBUI.size(Int.MAX_VALUE, 250)
    val html = htmlWithResources(project, taskText, task)
    taskSpecificJBCefBrowser.loadHTML(html)
    taskSpecificJBCefBrowser.component.isVisible = true
  }

  companion object {
    @TestOnly
    fun processContent(content: String, project: Project): String {
      return htmlWithResources(project, content)
    }
  }
}
