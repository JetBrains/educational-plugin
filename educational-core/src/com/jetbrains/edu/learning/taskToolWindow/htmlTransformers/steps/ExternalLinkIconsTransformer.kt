package com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.steps

import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.taskToolWindow.A_TAG
import com.jetbrains.edu.learning.taskToolWindow.HREF_ATTRIBUTE
import com.jetbrains.edu.learning.taskToolWindow.IMG_TAG
import com.jetbrains.edu.learning.taskToolWindow.SRC_ATTRIBUTE
import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.HtmlTransformer
import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.HtmlTransformerContext
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.StyleManager
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.StyleResourcesManager
import org.jetbrains.annotations.VisibleForTesting
import org.jsoup.nodes.Document

object ExternalLinkIconsTransformer : HtmlTransformer {
  override fun transform(html: Document, context: HtmlTransformerContext): Document {
    return addExternalLinkIcons(html)
  }
}

private const val STYLE_ATTRIBUTE = "style"
private const val SPAN_ATTRIBUTE = "span"
private const val HEIGHT_ATTRIBUTE = "height"
private const val WIDTH_ATTRIBUTE = "width"
private const val BORDER_ATTRIBUTE = "border"
private val EXTERNAL_LINK_REGEX = "https?://.*".toRegex()

private fun addExternalLinkIcons(document: Document): Document {
  val links = document.getElementsByTag(A_TAG)
  val externalLinks = links.filter { element -> element.attr(HREF_ATTRIBUTE).matches(EXTERNAL_LINK_REGEX) }
  val arrowIcon = if (UIUtil.isUnderDarcula()) {
    StyleResourcesManager.EXTERNAL_LINK_ARROW_DARK_PNG
  }
  else {
    StyleResourcesManager.EXTERNAL_LINK_ARROW_PNG
  }
  for (link in externalLinks) {
    val span = document.createElement(SPAN_ATTRIBUTE)
    link.replaceWith(span)
    span.appendChild(link)
    link.appendElement(IMG_TAG)
    val img = link.getElementsByTag(IMG_TAG)
    val fontSize = StyleManager().bodyFontSize
    val pictureSize = getPictureSize(fontSize)

    img.attr(SRC_ATTRIBUTE, StyleResourcesManager.resourceUrl(arrowIcon))
    img.attr(STYLE_ATTRIBUTE, "display:inline; position:relative; top:${fontSize * 0.18}; left:-${fontSize * 0.1}")
    img.attr(BORDER_ATTRIBUTE, "0")
    img.attr(WIDTH_ATTRIBUTE, pictureSize)
    img.attr(HEIGHT_ATTRIBUTE, pictureSize)
  }
  return document
}

@VisibleForTesting
fun getPictureSize(fontSize: Int): String {
  return if (JavaUILibrary.isSwing()) {
    fontSize
  }
  else {
    // rounding it to int is needed here, because if we are passing a float number, an arrow disappears in studio
    (fontSize * 1.2).toInt()
  }.toString()
}
