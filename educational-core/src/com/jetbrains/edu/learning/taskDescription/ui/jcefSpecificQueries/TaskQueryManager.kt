package com.jetbrains.edu.learning.taskDescription.ui.jcefSpecificQueries

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter

abstract class TaskQueryManager<T : Task>(
  protected val task: T,
  protected val taskJBCefBrowser: JBCefBrowserBase
): Disposable {
  open val queries: List<JBCefJSQuery>
    get() = listOf(jsQuerySetScrollHeight)

  private val jsQuerySetScrollHeight = JBCefJSQuery.create(taskJBCefBrowser)

  init {
    addScrollHeightHandler(taskJBCefBrowser)
  }

  private fun addScrollHeightHandler(browserBase: JBCefBrowserBase) {
    jsQuerySetScrollHeight.addHandler { height ->
      try {
        browserBase.component?.preferredSize = JBUI.size(Int.MAX_VALUE, height.toInt() + 20)
        browserBase.component?.revalidate()
      }
      catch (ignored: NumberFormatException) {
      }
      null
    }
  }

  protected fun registerQueries(parent: Disposable) {
    for (query in queries) {
      Disposer.register(parent, query)
    }
  }

  override fun dispose() {}

  protected abstract inner class TaskSpecificLoadHandler : CefLoadHandlerAdapter() {
    abstract val parentDocumentId: String
    override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
      browser?.mainFrame?.executeJavaScript(
        "var height = document.getElementById('$parentDocumentId').scrollHeight;" +
        jsQuerySetScrollHeight.inject("height"),
        browser.url, 0
      )
    }
  }
}