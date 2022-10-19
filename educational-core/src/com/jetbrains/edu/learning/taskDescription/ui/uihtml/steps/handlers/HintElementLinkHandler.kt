package com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.handlers

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.taskDescription.ui.SwingToolWindowLinkHandler
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.HINT_PROTOCOL
import java.io.IOException
import java.util.Enumeration
import javax.swing.JTextPane
import javax.swing.event.HyperlinkEvent
import javax.swing.text.AbstractDocument
import javax.swing.text.BadLocationException
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.html.HTML
import javax.swing.text.html.HTMLDocument

class HintElementLinkHandler(project: Project, private val textPane: JTextPane) : SwingToolWindowLinkHandler(project) {
  override fun processEvent(event: HyperlinkEvent) {
    val url = event.description
    if (url.startsWith(HINT_PROTOCOL)) {
      val sourceElement = event.sourceElement
      toggleHintElement(sourceElement)
      return
    }
    super.processEvent(event)
  }

  private fun toggleHintElement(sourceElement: javax.swing.text.Element) {
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
        EduCounterUsageCollector.hintExpanded()
      }
      else {
        val leftPath = if (UIUtil.isRetina()) "style/hint/swing/swing_icons/retina_right.png" else "style/hint/swing/swing_icons/right.png"
        changeArrowIcon(sourceElement, document, leftPath)
        document.removeElement(hintTextElement)
        EduCounterUsageCollector.hintCollapsed()
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
  private fun changeArrowIcon(sourceElement: javax.swing.text.Element, document: HTMLDocument, iconUrl: String) {
    val resource = javaClass.classLoader.getResource(iconUrl)
    if (resource != null) {
      val arrowImageElement = getArrowImageElement(sourceElement.parentElement)
      document.setOuterHTML(arrowImageElement, String.format("<img src='%s' width='16' height='16'>", resource.toExternalForm()))
    }
    else {
      LOG.warn("Cannot find arrow icon $iconUrl")
    }
  }

  private fun getArrowImageElement(element: javax.swing.text.Element): javax.swing.text.Element? {
    var result: javax.swing.text.Element? = null
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

  private fun getHintTextElement(parent: javax.swing.text.Element): javax.swing.text.Element? {
    var hintTextElement: javax.swing.text.Element? = null
    val children: Enumeration<*> = (parent as AbstractDocument.AbstractElement).parent.children()
    while (children.hasMoreElements()) {
      val child = children.nextElement() as javax.swing.text.Element
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
    private val LOG = Logger.getInstance(HintElementLinkHandler::class.java)

    // All tagged elements should have different href otherwise they are all underlined on hover.
    // That's why we have to add hint number to href
    private const val HINT_TEXT_PATTERN = "<div class='hint_text'>%s</div>"
  }
}
