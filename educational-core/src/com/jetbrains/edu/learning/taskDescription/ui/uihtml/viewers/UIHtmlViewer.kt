package com.jetbrains.edu.learning.taskDescription.ui.uihtml.viewers

import com.intellij.openapi.Disposable
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformerContext
import javax.swing.JComponent

abstract class UIHtmlViewer : Disposable {

  fun setHtmlWithContext(context: HtmlTransformerContext) = setHtmlWithContext("", context)

  abstract fun setHtmlWithContext(html: String, context: HtmlTransformerContext)

  abstract val component: JComponent

  override fun dispose() {}
}