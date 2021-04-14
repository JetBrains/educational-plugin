package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.Companion.hintCollapsed
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.Companion.hintExpanded
import java.io.IOException
import java.util.*
import javax.swing.JTextPane
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener
import javax.swing.text.AbstractDocument
import javax.swing.text.BadLocationException
import javax.swing.text.Element
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.html.HTML
import javax.swing.text.html.HTMLDocument


open class SwingToolWindowLinkHandler(project: Project, private val textPane: JTextPane) : ToolWindowLinkHandler(project) {
  val hyperlinkListener: HyperlinkListener
    get() = HyperlinkListener { event -> processEvent(event) }

  private fun processEvent(event: HyperlinkEvent) {
    if (event.eventType != HyperlinkEvent.EventType.ACTIVATED) {
      return
    }
    val url = event.description
    if (url.startsWith(HINT_PROTOCOL)) {
      val sourceElement = event.sourceElement
      toggleHintElement(sourceElement)
      return
    }
    process(url)
  }

  private fun toggleHintElement(sourceElement: Element) {
    try {
      val document = textPane.document as HTMLDocument
      val parent = sourceElement.parentElement
      val className = parent.parentElement.attributes.getAttribute(HTML.Attribute.CLASS) as String
      if ("hint" != className) {
        LOG.warn(
          String.format("Div element with hint class not found. Course: %s", StudyTaskManager.getInstance(project).course))
        return
      }
      val hintTextElement = getHintTextElement(parent)
      if (hintTextElement == null) {
        val downPath = if (UIUtil.isRetina()) "style/hint/swing/swing_icons/retina_down.png" else "style/hint/swing/swing_icons/down.png"
        changeArrowIcon(sourceElement, document, downPath)
        val hintText = (sourceElement.attributes.getAttribute(HTML.Tag.A) as SimpleAttributeSet).getAttribute(HTML.Attribute.VALUE)
        document.insertBeforeEnd(parent.parentElement, String.format(HINT_TEXT_PATTERN, hintText))
        hintExpanded()
      }
      else {
        val leftPath = if (UIUtil.isRetina()) "style/hint/swing/swing_icons/retina_right.png" else "style/hint/swing/swing_icons/right.png"
        changeArrowIcon(sourceElement, document, leftPath)
        document.removeElement(hintTextElement)
        hintCollapsed()
      }
    }
    catch (e: BadLocationException) {
      LOG.warn(e.message)
    }
    catch (e: IOException) {
      LOG.warn(e.message)
    }
  }

  @Throws(BadLocationException::class, IOException::class)
  private fun changeArrowIcon(sourceElement: Element, document: HTMLDocument, iconUrl: String) {
    val resource = javaClass.classLoader.getResource(iconUrl)
    if (resource != null) {
      val arrowImageElement = getArrowImageElement(sourceElement.parentElement)
      document.setOuterHTML(arrowImageElement, String.format("<img src='%s' width='16' height='16'>", resource.toExternalForm()))
    }
    else {
      LOG.warn("Cannot find arrow icon $iconUrl")
    }
  }

  private fun getArrowImageElement(element: Element): Element? {
    var result: Element? = null
    for (i in 0 until element.elementCount) {
      val child = element.getElement(i) ?: continue
      val attributes = child.attributes ?: continue
      val img = attributes.getAttribute(HTML.Attribute.SRC) as? String ?: continue
      if (img.endsWith("down.png") || img.endsWith("right.png")) {
        result = child
      }
    }
    return result
  }

  private fun getHintTextElement(parent: Element): Element? {
    var hintTextElement: Element? = null
    val children: Enumeration<*> = (parent as AbstractDocument.AbstractElement).parent.children()
    while (children.hasMoreElements()) {
      val child = children.nextElement() as Element
      val childAttributes = child.attributes
      val childClassName = childAttributes.getAttribute(HTML.Attribute.CLASS) as String
      if (childClassName == "hint_text") {
        hintTextElement = child
        break
      }
    }
    return hintTextElement
  }

  companion object {
    private const val HINT_PROTOCOL = "hint://"
    private const val HINT_TEXT_PATTERN = "<div class='hint_text'>%s</div>"
    private val LOG = Logger.getInstance(SwingToolWindowLinkHandler::class.java)
  }
}