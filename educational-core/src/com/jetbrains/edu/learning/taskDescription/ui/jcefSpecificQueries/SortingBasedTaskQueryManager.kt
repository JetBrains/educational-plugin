package com.jetbrains.edu.learning.taskDescription.ui.jcefSpecificQueries

import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingBasedTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter

class SortingBasedTaskQueryManager(
  task: SortingBasedTask,
  taskJBCefBrowser: JBCefBrowserBase
) : TaskQueryManager<SortingBasedTask>(task, taskJBCefBrowser) {
  private val jsQueryGetOrdering = JBCefJSQuery.create(taskJBCefBrowser)

  init {
    addOrderingHandler()
  }

  private fun addOrderingHandler() {
    jsQueryGetOrdering.addHandler { query ->
      if (query.isBlank()) return@addHandler null
      try {
        val values = query.split(" ").map { it.toInt() }.toIntArray()
        currentTask.ordering = values
      }
      catch (ignored: NumberFormatException) {
      }
      null
    }
  }

  override fun getTaskSpecificLoadHandler(): CefLoadHandlerAdapter = object : TaskSpecificLoadHandler() {
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
          """.trimIndent(), jcefBrowserUrl, 0
      )
    }
  }
}