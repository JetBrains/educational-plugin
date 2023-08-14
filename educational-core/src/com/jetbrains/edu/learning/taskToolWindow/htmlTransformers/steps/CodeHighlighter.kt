package com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.steps

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.taskToolWindow.ui.EduCodeHighlighter.Companion.highlightCodeFragments
import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.HtmlTransformer
import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.HtmlTransformerContext
import org.jsoup.nodes.Document

object CodeHighlighter : HtmlTransformer {
  override fun transform(html: Document, context: HtmlTransformerContext): Document {
    val task = context.task
    val project = context.project

    val course = task.course
    val language = if (course is HyperskillCourse) PlainTextLanguage.INSTANCE else course.languageById ?: return html

    return highlightCodeFragments(project, html, language)
  }
}