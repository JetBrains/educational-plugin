package com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps

import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.ui.jcef.JCEFHtmlPanel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.ChoiceTaskResourcesManager
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformer
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformerContext
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.viewers.JcefUIHtmlViewer
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter

// We are sure that the ChoiceTaskTransformer is always called with the non-null task of type ChoiceTask.
// To make it type safe, it is possible to make HtmlTransformers have a <Context> type parameter.
// But this greatly complicates the code.
private val HtmlTransformerContext.choiceTask : ChoiceTask
  get() = task as ChoiceTask

// should we also do this lowercase, as other transformers?
object ChoiceTaskTransformer : HtmlTransformer {
  override fun swingTransform(html: String, context: HtmlTransformerContext): String = context.choiceTask.quizHeader

  override fun jcefTransform(html: String, context: HtmlTransformerContext): String = ChoiceTaskResourcesManager.getText(context.choiceTask)

  override fun setupJcefPanel(project: Project, htmlViewer: JcefUIHtmlViewer) {
    val htmlPane = htmlViewer.htmlPane

    val jsQueryGetChosenTasks = JBCefJSQuery.create(htmlPane as JBCefBrowserBase)
    val jsQuerySetScrollHeight = JBCefJSQuery.create(htmlPane as JBCefBrowserBase)

    jsQuerySetScrollHeight.addHandler { height ->
      try {
        htmlPane.component.preferredSize = JBUI.size(Int.MAX_VALUE, height.toInt() + 20)
        htmlPane.component.revalidate()
      }
      catch (ignored: NumberFormatException) {
      }
      null
    }

    jsQueryGetChosenTasks.addHandler { query ->
      val currentTask = htmlViewer.context?.choiceTask

      if (query.isBlank()) return@addHandler null
      if (currentTask == null) return@addHandler null
      try {
        val values = query.split(",").map { it.toInt() }.toMutableList()
        currentTask.selectedVariants = values
      }
      catch (ignored: NumberFormatException) {
      }
      null
    }

    htmlPane.jbCefClient.addLoadHandler(
      TaskSpecificLoadHandler(jsQueryGetChosenTasks, jsQuerySetScrollHeight, htmlPane),
      htmlPane.cefBrowser
    )
  }

  private class TaskSpecificLoadHandler(
    val jsQueryGetChosenTasks: JBCefJSQuery,
    val jsQuerySetScrollHeight: JBCefJSQuery,
    val htmlViewer: JCEFHtmlPanel
  ) : CefLoadHandlerAdapter() {
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
          """.trimIndent(), htmlViewer.cefBrowser.url, 0
      )
    }
  }

}