package com.jetbrains.edu.learning.checkio.checker

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefJSQuery
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefLoadHandlerAdapter
import javax.swing.JComponent

class JCEFCheckiOMissionCheck(project: Project,
                              task: Task,
                              oAuthConnector: CheckiOOAuthConnector,
                              interpreterName: String,
                              testFormTargetUrl: String
) : CheckiOMissionCheck(project, task, oAuthConnector, interpreterName, testFormTargetUrl) {
  private val jbCefBrowser = JBCefBrowser()
  private val jbCefJSQuery = JBCefJSQuery.create(jbCefBrowser)

  init {
    jbCefBrowser.cefBrowser.createImmediately()
    jbCefBrowser.jbCefClient.addLoadHandler(TestFormLoadHandler(), jbCefBrowser.cefBrowser)

    jbCefJSQuery.addHandler { value ->
      val result = value.toIntOrNull() ?: return@addHandler null

      ApplicationManager.getApplication().executeOnPooledThread {
        setCheckResult(result)
      }
      null
    }
    // TODO: pass another disposable with shorter lifetime
    // for example, content manager of the corresponding tool window
    // otherwise, jcef browser objects can be leaked until the project is closed.
    // But it's better than nothing
    Disposer.register(StudyTaskManager.getInstance(project), jbCefBrowser)
  }

  override fun doCheck(resources: Map<String, String>) {
    invokeLater {
      val html = getTestFormHtml(resources)
      jbCefBrowser.loadHTML(html)
    }
  }

  override fun getPanel(): JComponent = jbCefBrowser.component

  private inner class TestFormLoadHandler : CefLoadHandlerAdapter() {
    override fun onLoadError(browser: CefBrowser?,
                             frame: CefFrame?,
                             errorCode: CefLoadHandler.ErrorCode?,
                             errorText: String?,
                             failedUrl: String?) {
      ApplicationManager.getApplication().executeOnPooledThread {
        setConnectionError()
      }
    }

    override fun onLoadEnd(browser: CefBrowser, frame: CefFrame?, httpStatusCode: Int) {
      // actually it means test form
      if (browser.url.contains("about:blank")) {
        browser.mainFrame.executeJavaScript(
          """
          form = document.getElementById('test-form');
          if (form) {
            form.hidden = true;
            form.submit();
          }
          """.trimIndent(), jbCefBrowser.cefBrowser.url, 0
        )
      }

      if (browser.url.contains("check-html-output")) {
        browser.mainFrame.executeJavaScript(
          """
          function handleEvent(e) {
            let value = e.detail.success;
            ${jbCefJSQuery.inject("value")}
          }
          window.addEventListener('checkio:checkDone', handleEvent, false);  
            
          document.documentElement.setAttribute("style", "background-color : #DEE7F6;")
          """.trimIndent(), jbCefBrowser.cefBrowser.url, 0
        )
      }
    }
  }
}
