package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.codeEditor.printing.HTMLTextPainter
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

class EduCodeHighlighter {

  companion object {
    @JvmStatic
    fun highlightCodeFragments(project: Project, html: String, defaultLanguage: Language): String {
      val document = Jsoup.parse(html)

      val codeElements = document.select("code")

      for (codeElement in codeElements) {
        val textNode = codeElement.childNodes().singleOrNull() as? TextNode ?: continue
        val text = textNode.wholeText
        val language = codeElement.language() ?: defaultLanguage

        val psiFile = PsiFileFactory.getInstance(project).createFileFromText(language, "")
        if (psiFile == null) return html

        val codeText = HTMLTextPainter.convertCodeFragmentToHTMLFragmentWithInlineStyles(psiFile, text)

        val parent = codeElement.parent()
        // We have to check `parent.parent()` for null
        // because in case of incomplete code `parent.parent()` can be null
        // and in this case `parent.after(codeText)` throws `IllegalArgumentException`
        if (parent.tagName() == "pre" && parent.parent() != null) {
          parent.after(codeText)
          parent.remove()
        } else {
          val inlineCodeText = codeText.trim().removeSurrounding("<pre>", "</pre>")
          codeElement.after(inlineCodeText)
          codeElement.remove()
        }
      }

      return document.toString()
    }

    private fun Element.language(): Language? {
      val lang = when {
        hasAttr("data-lang") -> attr("data-lang").removePrefix("text/x-")
        attr("class").startsWith("language-") -> attr("class").removePrefix("language-")
        else -> return null
      }
      if (lang.isEmpty()) return null

      return Language.getRegisteredLanguages().find { it.id.toLowerCase() == lang }
    }
  }
}
