package com.jetbrains.edu.learning.taskToolWindow.ui.jcefSpecificQueries

import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import com.jetbrains.edu.learning.courseFormat.tasks.TableTask
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame

class TableTaskQueryManager(
  task: TableTask,
  taskJBCefBrowser: JBCefBrowserBase
) : TaskQueryManager<TableTask>(task, taskJBCefBrowser) {
  private val jsQueryGetSelection = JBCefJSQuery.create(taskJBCefBrowser)

  override val queries: List<JBCefJSQuery> = super.queries.plus(jsQueryGetSelection)

  private val taskSpecificLoadHandler = object : TaskSpecificLoadHandler() {
    override val parentDocumentId: String = "tableTask"

    override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
      super.onLoadEnd(browser, frame, httpStatusCode)

      browser?.mainFrame?.executeJavaScript(
        """
          inputs = document.getElementsByTagName('input');
          [].slice.call(inputs).forEach(input => {
            input.addEventListener('change', function (event) {
              let value = getSelection();
              ${jsQueryGetSelection.inject("value")}
            })
          })
          """.trimIndent(), taskJBCefBrowser.cefBrowser.url, 0
      )
    }
  }

  init {
    addSelectionHandler()
    Disposer.register(this, jsQueryGetSelection)
    taskJBCefBrowser.jbCefClient.addLoadHandler(taskSpecificLoadHandler, taskJBCefBrowser.cefBrowser)
  }

  private fun addSelectionHandler() {
    jsQueryGetSelection.addHandler { query ->
      if (query.isBlank()) return@addHandler null
      try {
        val values = query.split(",").map { row ->
          row.split(" ").map { it == "1" }.toBooleanArray()
        }.toTypedArray()
        println(values.joinToString("\n") { it.joinToString(" ") })
        task.selected = values
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