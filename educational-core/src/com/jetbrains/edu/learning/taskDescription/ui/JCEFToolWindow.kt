package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.ui.jcef.JCEFHtmlPanel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.ChoiceTaskResourcesManager
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import org.jetbrains.annotations.TestOnly
import org.jsoup.nodes.Element
import javax.swing.JComponent

@Suppress("UnstableApiUsage")
class JCEFToolWindow(project: Project) : TaskDescriptionToolWindow(project) {
  private val taskInfoJBCefBrowser = JCEFHtmlPanel(JBCefApp.getInstance().createClient(), null)
  private val taskSpecificJBCefBrowser = JCEFHtmlPanel(JBCefApp.getInstance().createClient(), null)
  private var currentTask: ChoiceTask? = null

  private val jsQueryGetChosenTasks = JBCefJSQuery.create(taskSpecificJBCefBrowser)
  private val jsQuerySetScrollHeight = JBCefJSQuery.create(taskSpecificJBCefBrowser)

  init {
    // BACKCOMPAT: 2020.3: error page is disable by default in 211 branch
    taskInfoJBCefBrowser.disableErrorPage()

    val jcefLinkInToolWindowHandler = JCefToolWindowLinkHandler(project)
    val taskInfoRequestHandler = JCEFToolWindowRequestHandler(jcefLinkInToolWindowHandler)
    taskInfoJBCefBrowser.jbCefClient.addRequestHandler(taskInfoRequestHandler, taskInfoJBCefBrowser.cefBrowser)
    val taskInfoLifeSpanHandler = JCEFTaskInfoLifeSpanHandler(jcefLinkInToolWindowHandler)
    taskInfoJBCefBrowser.jbCefClient.addLifeSpanHandler(taskInfoLifeSpanHandler, taskInfoJBCefBrowser.cefBrowser)
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
                 LafManagerListener { TaskDescriptionView.updateAllTabs(project) })
  }

  override val taskInfoPanel: JComponent
    get() = taskInfoJBCefBrowser.component

  override val taskSpecificPanel: JComponent
    get() = taskSpecificJBCefBrowser.component

  override fun wrapHint(hintElement: Element, displayedHintNumber: String): String {
    return wrapHint(project, hintElement, displayedHintNumber)
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

  companion object {
    private const val HINT_HEADER: String = "hint_header"
    private const val HINT_HEADER_EXPANDED: String = "$HINT_HEADER checked"
    private const val HINT_BLOCK_TEMPLATE: String = "<div class='" + HINT_HEADER + "'>Hint %s</div>" +
                                                    "  <div class='hint_content'>" +
                                                    " %s" +
                                                    "  </div>"
    private const val HINT_EXPANDED_BLOCK_TEMPLATE: String = "<div class='" + HINT_HEADER_EXPANDED + "'>Hint %s</div>" +
                                                             "  <div class='hint_content'>" +
                                                             " %s" +
                                                             "  </div>"

    fun wrapHint(project: Project, hintElement: Element, displayedHintNumber: String): String {
      val course = StudyTaskManager.getInstance(project).course
      val hintText: String = hintElement.html()
      if (course == null) {
        return String.format(HINT_BLOCK_TEMPLATE, displayedHintNumber, hintText)
      }

      val study = course.isStudy
      return if (study) {
        String.format(HINT_BLOCK_TEMPLATE, displayedHintNumber, hintText)
      }
      else {
        String.format(HINT_EXPANDED_BLOCK_TEMPLATE, displayedHintNumber, hintText)
      }
    }

    @TestOnly
    fun processContent(content: String, project: Project): String {
      return htmlWithResources(project, content)
    }
  }
}
