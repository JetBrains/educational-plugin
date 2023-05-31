package com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.taskDescription.ui.EduCodeHighlighter.Companion.highlightCodeFragments
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformerContext
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.UIModeIndependentHtmlTransformer

object CodeHighlighter : UIModeIndependentHtmlTransformer() {
  override fun transform(html: String, context: HtmlTransformerContext): String {
    val task = context.task
    task ?: return html
    val project = context.project

    val course = task.course
    val language = if (course is HyperskillCourse) PlainTextLanguage.INSTANCE else course.languageById ?: return html

    return highlightCodeFragments(project, html, language)
  }
}