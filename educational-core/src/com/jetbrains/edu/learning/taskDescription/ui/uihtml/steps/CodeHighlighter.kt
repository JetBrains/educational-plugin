package com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps

import com.intellij.codeEditor.printing.HTMLTextPainter
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.UIModeIndependentHtmlTransformer
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformerContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

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

private fun highlightCodeFragments(project: Project, html: String, defaultLanguage: Language): String {
  val document = Jsoup.parse(html)

  val codeElements = document.select("code")

  for (codeElement in codeElements) {
    val textNode = codeElement.childNodes().singleOrNull() as? TextNode ?: continue
    val text = textNode.wholeText
    val language = codeElement.language() ?: defaultLanguage

    val psiFile = PsiFileFactory.getInstance(project).createFileFromText(language, "")
    psiFile ?: return html

    val codeText = HTMLTextPainter.convertCodeFragmentToHTMLFragmentWithInlineStyles(psiFile, text)

    val parent = codeElement.parent()
    // We have to check `parent.parent()` for null
    // because in case of incomplete code `parent.parent()` can be null
    // and in this case `parent.after(codeText)` throws `IllegalArgumentException`
    if (parent?.tagName() == "pre" && parent.parent() != null) {
      parent.after("<span class='code-block'>$codeText</span>")
      parent.remove()
    } else {
      val inlineCodeText = codeText.trim().removeSurrounding("<pre>", "</pre>")
      codeElement.after("<span class='code'>$inlineCodeText</span>")
      codeElement.remove()
    }
  }

  return document.toString()
}

private fun Element.language(): Language? {
  val noHighlight = "no-highlight"

  val lang = when {
    hasAttr("data-lang") -> attr("data-lang").removePrefix("text/x-")
    attr("class").startsWith("language-") -> attr("class").removePrefix("language-")
    attr("class") == noHighlight -> return PlainTextLanguage.INSTANCE
    else -> return null
  }
  if (lang.isEmpty()) return null

  return if (lang == noHighlight) PlainTextLanguage.INSTANCE else Language.getRegisteredLanguages().find { it.id.lowercase() == lang }
}
