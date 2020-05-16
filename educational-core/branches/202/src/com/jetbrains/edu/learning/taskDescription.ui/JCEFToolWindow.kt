package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.stepik.StepikNames.STEPIK_URL
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.ChoiceTaskResourcesManager
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.network.CefRequest
import java.util.regex.Matcher
import javax.swing.JComponent

@Suppress("UnstableApiUsage")
class JCEFToolWindow(project: Project) : TaskDescriptionToolWindow(project) {
  private val taskInfoJBCefBrowser = JBCefBrowser()
  private val taskSpecificJBCefBrowser = JBCefBrowser()
  private var currentTask: ChoiceTask? = null

  private val jsQueryGetChosenTasks = JBCefJSQuery.create(taskSpecificJBCefBrowser)
  private val jsQuerySetScrollHeight = JBCefJSQuery.create(taskSpecificJBCefBrowser)

  init {
    taskInfoJBCefBrowser.jbCefClient.addRequestHandler(TaskInfoRequestHandler(project), taskInfoJBCefBrowser.cefBrowser)

    taskSpecificJBCefBrowser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
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
    }, taskSpecificJBCefBrowser.cefBrowser)

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

  override fun updateLaf() {}

  private class TaskInfoRequestHandler(val project: Project) : CefRequestHandlerAdapter() {
    override fun onBeforeBrowse(browser: CefBrowser?,
                                frame: CefFrame?,
                                request: CefRequest?,
                                user_gesture: Boolean,
                                is_redirect: Boolean): Boolean {
      var url = request?.url ?: return false
      if (url.contains("about:blank")) return false

      var result = false

      val a = "file:///intellij/jbcefbrowser/"
      if (url.contains(a)) {
        url = url.substringAfter(a)
      }

      object : LinkInToolWindowHandler(project) {
        override fun inCourseLinkHandler(matcher: Matcher) {
          super.inCourseLinkHandler(matcher)
          result = true
        }

        override fun psiElementLinkHandler(url: String) {
          super.psiElementLinkHandler(url)
          result = true
        }

        override fun externalLinkHandler(url: String) {
          var urlToOpen = url
          if (url.startsWith("file://")) {
            urlToOpen = STEPIK_URL + url.substringAfter("file://")
          }
          super.externalLinkHandler(urlToOpen)
          result = true
        }
      }.process(url)

      return result
    }
  }
}
