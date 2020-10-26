@file:JvmName("TaskDescriptionUtil")

package com.jetbrains.edu.learning.taskDescription

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.mimeType
import com.jetbrains.edu.learning.stepik.SOURCE
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleResourcesManager
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleResourcesManager.EXTERNAL_LINK_ARROW_DARK_PNG
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleResourcesManager.EXTERNAL_LINK_ARROW_PNG
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

private const val SHORTCUT = "shortcut"
private const val SHORTCUT_ENTITY = "&$SHORTCUT:"
private const val SHORTCUT_ENTITY_ENCODED = "&amp;$SHORTCUT:"
private const val VIDEO_TAG = "video"
private const val IFRAME_TAG = "iframe"
private const val YOUTUBE_VIDEO_ID_LENGTH = 11
private const val A_TAG = "a"
const val IMG_TAG = "img"
const val SCRIPT_TAG = "script"
const val SRC_ATTRIBUTE = "src"
private const val HEIGHT_ATTRIBUTE = "height"
private const val HREF_ATTRIBUTE = "href"
private const val STYLE_ATTRIBUTE = "style"
private const val SRCSET_ATTRIBUTE = "srcset"
private const val WIDTH_ATTRIBUTE = "width"
private const val BORDER_ATTRIBUTE = "border"
private const val IMAGE_TYPE = "image"
private const val DARK_SUFFIX = "_dark"
private val LOG: Logger = Logger.getInstance("com.jetbrains.edu.learning.taskDescription.utils")
private val HYPERSKILL_TAGS = tagsToRegex({ "\\[$it](.*)\\[/$it]" }, "HINT", "PRE", "META") +
                              tagsToRegex({ "\\[$it-\\w+](.*)\\[/$it]" }, "ALERT")
private val YOUTUBE_LINKS_REGEX = "https?://(www\\.)?(youtu\\.be|youtube\\.com)/?(watch\\?v=|embed)?.*".toRegex()
private val EXTERNAL_LINK_REGEX = "https?://.*".toRegex()

private fun tagsToRegex(pattern: (String) -> String, vararg tags: String): List<Regex> = tags.map { pattern(it).toRegex() }

// see EDU-2444
fun removeHyperskillTags(text: StringBuffer) {
  var result: String = text.toString()
  for (regex in HYPERSKILL_TAGS) {
    result = result.replace(regex) { it.groupValues[1] }
  }

  text.delete(0, text.length)
  text.append(result)
}

fun replaceActionIDsWithShortcuts(text: StringBuffer) {
  var lastIndex = 0
  while (lastIndex < text.length) {
    lastIndex = text.indexOf(SHORTCUT_ENTITY, lastIndex)
    var shortcutEntityLength = SHORTCUT_ENTITY.length
    if (lastIndex < 0) {
      //`&` symbol might be replaced with `&amp;`
      lastIndex = text.indexOf(SHORTCUT_ENTITY_ENCODED)
      if (lastIndex < 0) {
        return
      }
      shortcutEntityLength = SHORTCUT_ENTITY_ENCODED.length
    }
    val actionIdStart = lastIndex + shortcutEntityLength
    val actionIdEnd = text.indexOf(";", actionIdStart)
    if (actionIdEnd < 0) {
      return
    }
    val actionId = text.substring(actionIdStart, actionIdEnd)
    var shortcutText = KeymapUtil.getFirstKeyboardShortcutText(actionId)
    if (shortcutText.isEmpty()) {
      shortcutText = "<no shortcut for action $actionId>"
    }
    text.replace(lastIndex, actionIdEnd + 1, shortcutText)
    lastIndex += shortcutText.length
  }
}


fun processYoutubeLink(text: String, taskId: Int): String {
  val document = Jsoup.parse(text)
  val videoElements = document.getElementsByTag(VIDEO_TAG)
  for (element in videoElements) {
    val sourceElements = element.getElementsByTag(SOURCE)
    if (sourceElements.size != 1) {
      LOG.warn("Incorrect number of youtube video sources for task ${taskId}")
      continue
    }
    val src = sourceElements.attr(SRC_ATTRIBUTE)
    val elementToReplaceWith = getClickableImageElement(src, taskId) ?: continue
    element.replaceWith(elementToReplaceWith)
  }
  val iframeElements = document.getElementsByTag(IFRAME_TAG)
  for (element in iframeElements) {
    val src = element.attr(SRC_ATTRIBUTE)
    val elementToReplace = getClickableImageElement(src, taskId) ?: continue
    element.replaceWith(elementToReplace)
  }
  return document.outerHtml()
}

private fun getClickableImageElement(src: String, taskId: Int): Element? {
  val youtubeVideoId = src.getYoutubeVideoId()
  if (youtubeVideoId == null) {
    LOG.warn("Incorrect youtube video link ${src} for task ${taskId}")
    return null
  }
  val textToReplace = "<a href=http://www.youtube.com/watch?v=${youtubeVideoId}><img src=http://img.youtube.com/vi/${youtubeVideoId}/0.jpg></a>"
  val documentToReplace = Jsoup.parse(textToReplace)
  val elements = documentToReplace.getElementsByTag("a")
  return if (elements.isNotEmpty()) {
    elements[0]
  }
  else null
}

fun String.getYoutubeVideoId(): String? {
  if (!YOUTUBE_LINKS_REGEX.matches(this)) {
    return null
  }
  val splitLink = this.split("?v=", "/embed/", ".be/", "&", "?")
  return if (splitLink.size >= 2) {
    val id = splitLink[1]
    return if (id.length == YOUTUBE_VIDEO_ID_LENGTH) id
    else null
  }
  else {
    null
  }
}

fun processImagesAndLinks(project: Project, task: Task, taskText: String): String {
  val documentWithImagesByTheme = replaceImagesForTheme(project, task, taskText)
  return addExternalLinkIcons(documentWithImagesByTheme)
}

fun replaceImagesForTheme(project: Project, task: Task, taskText: String): Document {
  val document = Jsoup.parse(taskText)
  val imageElements = document.getElementsByTag(IMG_TAG)
  for (element in imageElements) {
    val srcAttr = element.attr(SRC_ATTRIBUTE)
    val isDarkTheme = UIUtil.isUnderDarcula()
    if (isDarkTheme && task.containsLocalImages(project, srcAttr)) {
      val fileNameWithoutExtension = FileUtil.getNameWithoutExtension(srcAttr)
      val fileExtension = FileUtilRt.getExtension(srcAttr)
      val darkSrc = "$fileNameWithoutExtension$DARK_SUFFIX.$fileExtension"
      if (task.taskFiles.containsKey(darkSrc)) {
        element.attr(SRC_ATTRIBUTE, darkSrc)
      }
    }
    if (element.hasAttr(SRCSET_ATTRIBUTE)) {
      if (isDarkTheme) {
        element.attr(SRC_ATTRIBUTE, element.attr(SRCSET_ATTRIBUTE))
      }
      element.removeAttr(SRCSET_ATTRIBUTE)
    }
  }
  return document
}

fun addExternalLinkIcons(document: Document): String {
  val links = document.getElementsByTag(A_TAG)
  val externalLinks = links.filter { it.attr(HREF_ATTRIBUTE).matches(EXTERNAL_LINK_REGEX) }
  val arrowIcon = if (UIUtil.isUnderDarcula()) {
    EXTERNAL_LINK_ARROW_DARK_PNG
  }
  else {
    EXTERNAL_LINK_ARROW_PNG
  }
  for (link in externalLinks) {
    val pictureSize = StyleManager().bodyFontSize
    link.appendElement(IMG_TAG)
    val img = link.getElementsByTag(IMG_TAG)
    img.attr(SRC_ATTRIBUTE, StyleResourcesManager.resourceUrl(arrowIcon))
    img.attr(STYLE_ATTRIBUTE, "display:inline")
    img.attr(BORDER_ATTRIBUTE, "0")
    img.attr(WIDTH_ATTRIBUTE, pictureSize.toString())
    img.attr(HEIGHT_ATTRIBUTE, pictureSize.toString())
  }
  return document.toString()
}

private fun Task.containsLocalImages(project: Project, fileName: String): Boolean {
  val virtualFile = getTaskFile(fileName)?.getVirtualFile(project) ?: return false
  val mimeType = virtualFile.mimeType()
  return mimeType?.startsWith(IMAGE_TYPE) ?: false
}

fun Task.addHeader(tasksNumber: Int, text: String): String = buildString {
  appendln("<h1 style=\"margin-top: 15px\">${uiName.capitalize()} $index/$tasksNumber: $name</h1>")
  appendln(text)
}

fun String.containsYoutubeLink(): Boolean = contains(YOUTUBE_LINKS_REGEX)

fun String.replaceEncodedShortcuts() = this.replace(SHORTCUT_ENTITY_ENCODED, SHORTCUT_ENTITY)

fun String.toShortcut(): String = "${SHORTCUT_ENTITY}$this;"

fun String.containsShortcut(): Boolean = startsWith(SHORTCUT_ENTITY) || startsWith(SHORTCUT_ENTITY_ENCODED)

fun link(url: String, text: String, right: Boolean = false): String = """<a${if (right) " class=right " else " "}href="$url">$text</a>"""
