package com.jetbrains.edu.learning.taskDescription.ui.jcefSpecificQueries

import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingBasedTask
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame

class SortingBasedTaskQueryManager(
  task: SortingBasedTask,
  taskJBCefBrowser: JBCefBrowserBase
) : TaskQueryManager<SortingBasedTask>(task, taskJBCefBrowser) {
  private val jsQueryGetOrdering = JBCefJSQuery.create(taskJBCefBrowser)

  override val queries: List<JBCefJSQuery>
    get() = super.queries + listOf(jsQueryGetOrdering)

  init {
    addOrderingHandler()
    registerQueries(this)
  }

  private fun addOrderingHandler() {
    jsQueryGetOrdering.addHandler { query ->
      if (query.isBlank()) return@addHandler null
      try {
        val values = query.split(" ").map { it.toInt() }.toIntArray()
        task.ordering = values
      }
      catch (ignored: NumberFormatException) {
      }
      null
    }
  }

  private val taskSpecificLoadHandler = object : TaskSpecificLoadHandler() {
    override val parentDocumentId: String = "options"

    override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
      super.onLoadEnd(browser, frame, httpStatusCode)

      browser?.mainFrame?.executeJavaScript(
        """
          buttons = document.getElementsByTagName('button');
          [].slice.call(buttons).forEach(input => {
            input.addEventListener('click', function (event) {
              let value = getOrdering();
              ${jsQueryGetOrdering.inject("value")}
            })
          })
          cards = document.getElementsByClassName('value');
          [].slice.call(cards).forEach(input => {
            input.addEventListener('keydown', function (event) {
              if (event.key != 'ArrowUp' && event.key != 'ArrowDown') return;
              let value = getOrdering();
              ${jsQueryGetOrdering.inject("value")}
            })
          })
          """.trimIndent(), taskJBCefBrowser.cefBrowser.url, 0
      )
    }
  }

  init {
    taskJBCefBrowser.jbCefClient.addLoadHandler(taskSpecificLoadHandler, taskJBCefBrowser.cefBrowser)
  }

  override fun dispose() {
    taskJBCefBrowser.jbCefClient.removeLoadHandler(taskSpecificLoadHandler, taskJBCefBrowser.cefBrowser)
  }
}