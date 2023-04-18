package com.jetbrains.edu.learning.taskDescription.ui.jcefSpecificQueries

import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter

abstract class TaskQueryManager<T : Task>(task: T, taskJBCefBrowser: JBCefBrowserBase) {
  private val jsQuerySetScrollHeight = JBCefJSQuery.create(taskJBCefBrowser)

  protected val jcefBrowserUrl: String = taskJBCefBrowser.cefBrowser.url

  var currentTask: T = task

  init {
    addScrollHeightHandler(taskJBCefBrowser)
  }

  abstract fun getTaskSpecificLoadHandler(): CefLoadHandlerAdapter

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