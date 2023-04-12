package com.jetbrains.edu.learning.taskDescription.ui.jcefSpecificQueries

import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingBasedTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter

abstract class SortingBasedTaskQueryManager<T: SortingBasedTask>(
  task: T,
  taskJBCefBrowser: JBCefBrowserBase
) : TaskQueryManager<T>(task, taskJBCefBrowser) {
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

  protected abstract inner class SortingBasedTaskSpecificLoadHandler: TaskSpecificLoadHandler() {
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
          """.trimIndent(), jcefBrowserUrl, 0
      )
    }
  }
}

class SortingTaskQueryManager(
  task: SortingTask,
  taskJBCefBrowser: JBCefBrowserBase
): SortingBasedTaskQueryManager<SortingTask>(task, taskJBCefBrowser) {
  override fun getTaskSpecificLoadHandler(): CefLoadHandlerAdapter = object : SortingBasedTaskSpecificLoadHandler() {
    override val parentDocumentId: String = "sortingOptions"
  }
}

class MatchingTaskQueryManager(
  task: MatchingTask,
  taskJBCefBrowser: JBCefBrowserBase
): SortingBasedTaskQueryManager<MatchingTask>(task, taskJBCefBrowser) {
  override fun getTaskSpecificLoadHandler(): CefLoadHandlerAdapter = object : SortingBasedTaskSpecificLoadHandler() {
    override val parentDocumentId: String = "matchingOptions"
  }
}