package com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.steps

import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.taskToolWindow.ui.EduCodeHighlighter.Companion.highlightCodeFragments
import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.HtmlTransformer
import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.HtmlTransformerContext
import org.jsoup.nodes.Document

object CodeHighlighter : HtmlTransformer {
  override fun transform(html: Document, context: HtmlTransformerContext): Document {
    val task = context.task
    val project = context.project

    val language = task.course.languageById ?: return html

    return highlightCodeFragments(project, html, language)
  }
}