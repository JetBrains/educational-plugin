package com.jetbrains.edu.learning.taskDescription.ui.uihtml.viewers

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.taskDescription.ui.createTextPane
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.SwingHtmlTransformer
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformerContext
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.NoOperationTransformer
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.text.html.HTMLEditorKit

class SwingUIHtmlViewer(project: Project, private val htmlTransformer: SwingHtmlTransformer = NoOperationTransformer): UIHtmlViewer() {

  val textPane: JTextPane
  override val component = JPanel(BorderLayout())

  var context: HtmlTransformerContext? = null
    private set

  override fun setHtmlWithContext(html: String, context: HtmlTransformerContext) {
    this.context = context
    val processedHtml = htmlTransformer.swingTransform(html, context)
    textPane.text = processedHtml
  }

  init {
    // we are using HTMLEditorKit here because otherwise styles are not applied
    val editorKit = HTMLEditorKit()
    editorKit.styleSheet = null
    textPane = createTextPane(editorKit)
    val scrollPane = JBScrollPane(textPane)
    scrollPane.border = JBUI.Borders.empty()
    component.add(scrollPane, BorderLayout.CENTER)

    htmlTransformer.setupSwingPanel(project, this)
  }
}