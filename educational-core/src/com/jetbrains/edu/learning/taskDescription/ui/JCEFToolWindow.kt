package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems.FILE_PROTOCOL_PREFIX
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.stepik.StepikNames.getStepikUrl
import com.jetbrains.edu.learning.taskDescription.containsYoutubeLink
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.ChoiceTaskResourcesManager
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.network.CefRequest
import javax.swing.JComponent

@Suppress("UnstableApiUsage")
class JCEFToolWindow(project: Project) : TaskDescriptionToolWindow(project) {
  private val taskInfoJBCefBrowser = JBCefBrowser()
  private val taskSpecificJBCefBrowser = JBCefBrowser()
  private var currentTask: ChoiceTask? = null

  private val jsQueryGetChosenTasks = JBCefJSQuery.create(taskSpecificJBCefBrowser)
  private val jsQuerySetScrollHeight = JBCefJSQuery.create(taskSpecificJBCefBrowser)

  private val jcefLinkInToolWindowHandler by lazy {
    JCefToolWindowLinkHandler()
  }

  init {
    taskInfoJBCefBrowser.jbCefClient.addRequestHandler(TaskInfoRequestHandler(), taskInfoJBCefBrowser.cefBrowser)
    taskInfoJBCefBrowser.jbCefClient.addLifeSpanHandler(TaskInfoLifeSpanHandler(), taskInfoJBCefBrowser.cefBrowser)
    taskSpecificJBCefBrowser.jbCefClient.addLoadHandler(TaskSpecificLoadHandler(), taskSpecificJBCefBrowser.cefBrowser)

    jsQuerySetScrollHeight.addHandler { height ->
      try {
        taskSpecificJBCefBrowser.component.preferredSize = JBUI.size(Int.MAX_VALUE, height.toInt() + 20)
        taskSpecificJBCefBrowser.component.revalidate()
      }
      catch (ignored: NumberFormatException) {
      }
      null
    }

    jsQueryGetChosenTasks.addHandler { query ->
      if (query.isBlank()) return@addHandler null
      if (currentTask == null) return@addHandler null
      try {
        val values = query.split(",").map { it.toInt() }.toMutableList()
        currentTask?.selectedVariants = values
      }
      catch (ignored: NumberFormatException) {
      }
      null
    }

    Disposer.register(this, taskInfoJBCefBrowser)
    Disposer.register(this, taskSpecificJBCefBrowser)

    ApplicationManager.getApplication().messageBus.connect(this)
      .subscribe(LafManagerListener.TOPIC,
                 LafManagerListener { TaskDescriptionView.updateAllTabs(TaskDescriptionView.getInstance(project)) })
  }

  override fun createTaskInfoPanel(): JComponent {
    return taskInfoJBCefBrowser.component
  }

  override fun createTaskSpecificPanel(): JComponent {
    return taskSpecificJBCefBrowser.component
  }

  override fun setText(text: String, task: Task?) {
    val html = htmlWithResources(project, wrapHints(text, task))
    taskInfoJBCefBrowser.loadHTML(html)
  }

  override fun updateTaskSpecificPanel(task: Task?) {
    taskSpecificJBCefBrowser.component.isVisible = false
    if (task !is ChoiceTask) {
      return
    }

    currentTask = task

    taskSpecificJBCefBrowser.component.preferredSize = JBUI.size(Int.MAX_VALUE, 250)
    val html = htmlWithResources(project, ChoiceTaskResourcesManager().getText(task))
    taskSpecificJBCefBrowser.loadHTML(html)
    taskSpecificJBCefBrowser.component.isVisible = true
  }

  private inner class TaskInfoRequestHandler : CefRequestHandlerAdapter() {
    override fun onBeforeBrowse(browser: CefBrowser?,
                                frame: CefFrame?,
                                request: CefRequest?,
                                user_gesture: Boolean,
                                is_redirect: Boolean): Boolean {
      val url = request?.url ?: return false
      return jcefLinkInToolWindowHandler.process(url)
    }
  }

  private inner class TaskInfoLifeSpanHandler : CefLifeSpanHandlerAdapter() {
    override fun onBeforePopup(browser: CefBrowser?, frame: CefFrame?, targetUrl: String?, targetFrameName: String?): Boolean {
      if (targetUrl == null) return true
      return jcefLinkInToolWindowHandler.process(targetUrl)
    }
  }

  private inner class TaskSpecificLoadHandler : CefLoadHandlerAdapter() {
    override fun onLoadEnd(browser: CefBrowser, frame: CefFrame?, httpStatusCode: Int) {
      browser.mainFrame.executeJavaScript(
        "var height = document.getElementById('choiceOptions').scrollHeight;" +
        jsQuerySetScrollHeight.inject("height"),
        browser.url, 0
      )

      browser.mainFrame.executeJavaScript(
        """
          inputs = document.getElementsByTagName('input');
          [].slice.call(inputs).forEach(input => {
            input.addEventListener('change', function (event) {
              let value = getSelectedVariants();
              ${jsQueryGetChosenTasks.inject("value")}
            })
          })
          """.trimIndent(), taskSpecificJBCefBrowser.cefBrowser.url, 0
      )
    }
  }

  private inner class JCefToolWindowLinkHandler : ToolWindowLinkHandler(project) {
    override fun process(url: String): Boolean {
      return when {
        url.contains("about:blank") -> false
        url.startsWith(JCEF_URL_PREFIX) -> super.process(url.substringAfter(JCEF_URL_PREFIX))
        url.containsYoutubeLink() -> false
        else -> super.process(url)
      }
    }

    override fun processExternalLink(url: String): Boolean {
      val urlToOpen = when {
        url.startsWith(FILE_PROTOCOL_PREFIX) -> getStepikUrl() + url.substringAfter(FILE_PROTOCOL_PREFIX)
        else -> url
      }
      EduBrowser.getInstance().browse(urlToOpen)
      return true
    }
  }

  companion object {
    private const val JCEF_URL_PREFIX = "file:///jbcefbrowser/"
  }
}
