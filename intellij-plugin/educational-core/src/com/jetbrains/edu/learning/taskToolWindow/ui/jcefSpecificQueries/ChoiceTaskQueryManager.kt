package com.jetbrains.edu.learning.taskToolWindow.ui.jcefSpecificQueries

import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame

class ChoiceTaskQueryManager(task: ChoiceTask, taskJBCefBrowser: JBCefBrowserBase) : TaskQueryManager<ChoiceTask>(task, taskJBCefBrowser) {
  private val jsQueryGetChosenTasks = JBCefJSQuery.create(taskJBCefBrowser)

  override val queries: List<JBCefJSQuery> = super.queries.plus(jsQueryGetChosenTasks)

  private val taskSpecificLoadHandler = object : TaskSpecificLoadHandler() {
    override val parentDocumentId: String = "choiceOptions"

    override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
      super.onLoadEnd(browser, frame, httpStatusCode)

      browser?.mainFrame?.executeJavaScript(
        """
          inputs = document.getElementsByTagName('input');
          [].slice.call(inputs).forEach(input => {
            input.addEventListener('change', function (event) {
              let value = getSelectedVariants();
              ${jsQueryGetChosenTasks.inject("value")}
            })
          })
          """.trimIndent(), taskJBCefBrowser.cefBrowser.url, 0
      )
    }
  }

  init {
    addChosenTaskHandler()
    Disposer.register(this, jsQueryGetChosenTasks)
    taskJBCefBrowser.jbCefClient.addLoadHandler(taskSpecificLoadHandler, taskJBCefBrowser.cefBrowser)
  }

  private fun addChosenTaskHandler() {
    jsQueryGetChosenTasks.addHandler { query ->
      if (query.isBlank()) return@addHandler null
      try {
        val values = query.split(",").map { it.toInt() }.toMutableList()
        task.selectedVariants = values
      }
      catch (ignored: NumberFormatException) {
      }
      null
    }
  }

  override fun dispose() {
    taskJBCefBrowser.jbCefClient.removeLoadHandler(taskSpecificLoadHandler, taskJBCefBrowser.cefBrowser)
  }
}