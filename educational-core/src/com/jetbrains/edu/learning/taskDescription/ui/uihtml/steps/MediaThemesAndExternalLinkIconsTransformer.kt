package com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.taskDescription.A_TAG
import com.jetbrains.edu.learning.taskDescription.HREF_ATTRIBUTE
import com.jetbrains.edu.learning.taskDescription.IMG_TAG
import com.jetbrains.edu.learning.taskDescription.SRC_ATTRIBUTE
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleResourcesManager
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.UIModeIndependentHtmlTransformer
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformerContext
import org.jetbrains.annotations.VisibleForTesting
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object MediaThemesAndExternalLinkIconsTransformer : UIModeIndependentHtmlTransformer() {
  override fun transform(html: String, context: HtmlTransformerContext): String {
    val task = context.task ?: return html
    val mediaReplaced = replaceMediaForTheme(context.project, task, html)
    return addExternalLinkIcons(mediaReplaced)
  }
}

private const val DARK_SRC_CUSTOM_ATTRIBUTE = "dark-src"
private const val IFRAME_TAG = "iframe"
private const val SRCSET_ATTRIBUTE = "srcset"
private const val STYLE_ATTRIBUTE = "style"
private const val SPAN_ATTRIBUTE = "span"
private const val HEIGHT_ATTRIBUTE = "height"
private const val WIDTH_ATTRIBUTE = "width"
private const val BORDER_ATTRIBUTE = "border"
private const val DARK_SUFFIX = "_dark"
private val externalLinkRegex = "https?://.*".toRegex()

/**
 * This method replaces the `src` attributes for `<img>` and for `<iframe>` elements.
 * It does it only if the dark theme is currently used.
 * The value to substitute for the original src value is searched in the following steps:
 * 1. If the custom attribute `darkSrc` is set (written as `data-dark-src` inside HTML),
 * then its value is used. No further steps are done.
 * 2. For image (`<img>`), if the `srcset` attribute is set, then its value is used, no further steps are done.
 * 3. For image (`<img>`), if the link directs to a local files, for example `image.png`, and the file `image_dark.png`
 * is also present, then the link is updated to direct to this file with a `_dark` postfix.
 *
 * Note, that the `srcset` attribute is always removed, independently of whether the dark theme is used or not.
 */
@VisibleForTesting
fun replaceMediaForTheme(project: Project, task: Task, taskText: String): Document {
  val document = Jsoup.parse(taskText)

  val isDarkTheme = UIUtil.isUnderDarcula()

  val imageElements = document.getElementsByTag(IMG_TAG)
  for (element in imageElements) {
    updateImageElementAccordingToUiTheme(element, isDarkTheme, task, project)
  }

  if (isDarkTheme) {
    val iframeElements = document.getElementsByTag(IFRAME_TAG)
    for (element in iframeElements) {
      useDarkSrcCustomAttributeIfPresent(element)
    }
  }

  return document
}

private fun updateImageElementAccordingToUiTheme(element: Element, isDarkTheme: Boolean, task: Task, project: Project) {
  // Remove srcset attribute independently of the theme. Store its value
  val srcsetValue = if (element.hasAttr(SRCSET_ATTRIBUTE)) element.attr(SRCSET_ATTRIBUTE) else null
  if (srcsetValue != null) {
    element.removeAttr(SRCSET_ATTRIBUTE)
  }

  if (!isDarkTheme) return

  // First, try to use data-dark-src attribute
  if (useDarkSrcCustomAttributeIfPresent(element)) return

  //second, try to use srcset attribute
  if (srcsetValue != null) {
    element.attr(SRC_ATTRIBUTE, srcsetValue)
    return
  }

  //third, try to find a local image file with the _dark postfix
  val srcAttr = element.attr(SRC_ATTRIBUTE)

  val fileNameWithoutExtension = FileUtilRt.getNameWithoutExtension(srcAttr)
  val fileExtension = FileUtilRt.getExtension(srcAttr)
  val darkSrc = "$fileNameWithoutExtension$DARK_SUFFIX.$fileExtension"
  val taskDir = task.getDir(project.courseDir)?.path
  if (task.taskFiles.containsKey(darkSrc) || (taskDir != null && FileUtil.exists("$taskDir${VfsUtil.VFS_SEPARATOR_CHAR}$darkSrc"))) {
    element.attr(SRC_ATTRIBUTE, darkSrc)
  }
}

/**
 * Take the value for the `src` attribute from the `dataSrc` attribute (written as `data-dark-src` in HTML),
 * if the latter is present.
 * @return whether the replacement was made
 */
private fun useDarkSrcCustomAttributeIfPresent(element: Element): Boolean {
  val darkSrc = element.dataset()[DARK_SRC_CUSTOM_ATTRIBUTE]

  return if (darkSrc != null) {
    element.attr(SRC_ATTRIBUTE, darkSrc)
    true
  } else false
}

@VisibleForTesting
fun addExternalLinkIcons(document: Document): String {
  val links = document.getElementsByTag(A_TAG)
  val externalLinks = links.filter { element -> element.attr(HREF_ATTRIBUTE).matches(externalLinkRegex) }
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
  return document.toString()
}

@VisibleForTesting
fun getPictureSize(fontSize: Int): String {
  return if (JavaUILibrary.isSwing()) {
    fontSize
  }
  else {
    // rounding it to int is needed here, because, if we are passing a float number, an arrow disappears in studio
    (fontSize * 1.2).toInt()
  }.toString()
}