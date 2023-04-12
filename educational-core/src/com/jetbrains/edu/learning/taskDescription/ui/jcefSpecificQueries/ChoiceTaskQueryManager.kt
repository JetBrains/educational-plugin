package com.jetbrains.edu.learning.taskDescription.ui.jcefSpecificQueries

import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter

class ChoiceTaskQueryManager(task: ChoiceTask, taskJBCefBrowser: JBCefBrowserBase): TaskQueryManager<ChoiceTask>(task, taskJBCefBrowser) {
  private val jsQueryGetChosenTasks = JBCefJSQuery.create(taskJBCefBrowser)

  init {
    addChosenTaskHandler()
  }

  private fun addChosenTaskHandler() {
    jsQueryGetChosenTasks.addHandler { query ->
      if (query.isBlank()) return@addHandler null
      try {
        val values = query.split(",").map { it.toInt() }.toMutableList()
        currentTask.selectedVariants = values
      }
      catch (ignored: NumberFormatException) {
      }
      null
    }
  }

  override fun getTaskSpecificLoadHandler(): CefLoadHandlerAdapter = object : TaskSpecificLoadHandler() {
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
          """.trimIndent(), jcefBrowserUrl, 0
      )
    }
  }
}