package com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.steps

import com.intellij.ide.ui.UISettings
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.FontPreferences
import com.intellij.openapi.project.Project
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.HtmlTransformer
import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.HtmlTransformerContext
import com.jetbrains.edu.learning.taskToolWindow.ui.HINT_PROTOCOL
import com.jetbrains.edu.learning.taskToolWindow.ui.SwingToolWindow
import org.apache.commons.lang.StringEscapeUtils
import org.jetbrains.annotations.VisibleForTesting
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import kotlin.math.roundToInt

object HintsWrapper : HtmlTransformer {

  override fun transform(html: Document, context: HtmlTransformerContext): Document {
    if (context.task is TheoryTask) {
      return html
    }

    return wrapHintTagsInsideHTML(html) { e, number, title ->
      when (context.uiMode) {
        JavaUILibrary.JCEF -> wrapHintJCEF(context.project, e, number, title)
        JavaUILibrary.SWING -> wrapHintSwing(context.project, e, number, title)
        // all other options are deprecated, but we anyway should process them:
        else -> e.html()
      }
    }
  }
}

private const val HINT_HEADER: String = "hint_header"
private const val HINT_HEADER_EXPANDED: String = "$HINT_HEADER checked"
private const val HINT_BLOCK_TEMPLATE: String = "<div class='" + HINT_HEADER + "'>%s %s</div>" +
                                                "  <div class='hint_content'>" +
                                                " %s" +
                                                "  </div>"
private const val HINT_EXPANDED_BLOCK_TEMPLATE: String = "<div class='" + HINT_HEADER_EXPANDED + "'>%s %s</div>" +
                                                         "  <div class='hint_content'>" +
                                                         " %s" +
                                                         "  </div>"

private fun wrapHintJCEF(project: Project, hintElement: Element, displayedHintNumber: String, hintTitle: String): String {
  val course = StudyTaskManager.getInstance(project).course
  val hintText: String = hintElement.html()
  val escapedHintTitle = StringEscapeUtils.escapeHtml(hintTitle)
  if (course == null) {
    return String.format(HINT_BLOCK_TEMPLATE, escapedHintTitle, displayedHintNumber, hintText)
  }

  val study = course.isStudy
  return if (study) {
    String.format(HINT_BLOCK_TEMPLATE, escapedHintTitle, displayedHintNumber, hintText)
  }
  else {
    String.format(HINT_EXPANDED_BLOCK_TEMPLATE, escapedHintTitle, displayedHintNumber, hintText)
  }
}

private val LOG = Logger.getInstance(SwingToolWindow::class.java)  //TODO we probably need another logger here
private const val DEFAULT_ICON_SIZE = 16

private fun wrapHintSwing(project: Project, hintElement: Element, displayedHintNumber: String, hintTitle: String): String {

  fun getIconSize(): Int {
    val currentFontSize = UISettings.getInstance().fontSize
    val defaultFontSize = FontPreferences.DEFAULT_FONT_SIZE
    return (DEFAULT_ICON_SIZE * currentFontSize / defaultFontSize.toFloat()).roundToInt()
  }

  fun getIconFullPath(retinaPath: String, path: String): String {
    val bulbPath = if (UIUtil.isRetina()) retinaPath else path
    val bulbIconUrl = SwingToolWindow::class.java.classLoader.getResource(bulbPath)
    if (bulbIconUrl == null) {
      LOG.warn("Cannot find bulb icon")
    }
    return if (bulbIconUrl == null) "" else bulbIconUrl.toExternalForm()
  }

  fun getBulbIcon() = getIconFullPath("style/hint/swing/swing_icons/retina_bulb.png", "style/hint/swing/swing_icons/bulb.png")

  fun getLeftIcon() = getIconFullPath("style/hint/swing/swing_icons/retina_right.png", "style/hint/swing/swing_icons/right.png")

  fun getDownIcon() = getIconFullPath("style/hint/swing/swing_icons/retina_down.png", "style/hint/swing/swing_icons/down.png")

  // all tagged elements should have different href otherwise they are all underlined on hover. That's why
  // we have to add hint number to href
  fun createHintBlockTemplate(hintElement: Element, displayedHintNumber: String, escapedHintTitle: String): String {
    val iconSize = getIconSize()
    return """
      <img src='${getBulbIcon()}' width='$iconSize' height='$iconSize' >
      <span><a href='$HINT_PROTOCOL$displayedHintNumber', value='${hintElement.text()}'>$escapedHintTitle $displayedHintNumber</a>
      <img src='${getLeftIcon()}' width='$iconSize' height='$iconSize' >
    """.trimIndent()
  }

  // all tagged elements should have different href otherwise they are all underlined on hover. That's why
  // we have to add hint number to href
  fun createExpandedHintBlockTemplate(hintElement: Element, displayedHintNumber: String, escapedHintTitle: String): String {
    val hintText = hintElement.text()
    val iconSize = getIconSize()
    return """ 
        <img src='${getBulbIcon()}' width='$iconSize' height='$iconSize' >
        <span><a href='$HINT_PROTOCOL$displayedHintNumber', value='$hintText'>$escapedHintTitle $displayedHintNumber</a>
        <img src='${getDownIcon()}' width='$iconSize' height='$iconSize' >
        <div class='hint_text'>$hintText</div>
     """.trimIndent()
  }

  if (displayedHintNumber.isEmpty() || displayedHintNumber == "1") {
    hintElement.wrap("<div class='top'></div>")
  }
  val course = StudyTaskManager.getInstance(project).course
  val escapedHintTitle = StringEscapeUtils.escapeHtml(hintTitle)
  return if (course != null && !course.isStudy) {
    createExpandedHintBlockTemplate(hintElement, displayedHintNumber, escapedHintTitle)
  }
  else {
    createHintBlockTemplate(hintElement, displayedHintNumber, escapedHintTitle)
  }
}

@VisibleForTesting
fun wrapHintTagsInsideHTML(document: Document, wrapHint: (e: Element, number: String, title: String) -> String): Document {
  val hints = document.getElementsByClass("hint")

  val hintDefaultTitle = EduCoreBundle.message("course.creator.yaml.hint.default.title")

  fun getHintTitle(hint: Element): String {
    val actualTitleValue = hint.attr("title")
    return if (actualTitleValue == "") hintDefaultTitle else actualTitleValue
  }

  // map hint title to count of the same titles
  val countHintTitles = hints.groupingBy { getHintTitle(it) }.eachCount()

  val indexByTitle = mutableMapOf<String, Int>()

  for (hint in hints) {
    val hintTitle = getHintTitle(hint)
    val index = indexByTitle.getOrDefault(hintTitle, 0)
    val hintsWithThisTitle = countHintTitles.getValue(hintTitle)
    indexByTitle[hintTitle] = indexByTitle.getOrDefault(hintTitle, 0) + 1

    val textualIndex = if (hintsWithThisTitle <= 1) "" else (index + 1).toString()
    val hintText = wrapHint(hint, textualIndex, hintTitle)

    // we remove the title attribute, because otherwise it may generate popup hints
    hint.removeAttr("title")

    hint.html(hintText)
  }

  return document
}

