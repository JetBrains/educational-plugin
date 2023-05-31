/*
 * Copyright 2000-2021 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import org.jsoup.nodes.Element
import java.awt.BorderLayout
import java.io.IOException
import java.util.*
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.event.HyperlinkEvent
import javax.swing.text.AbstractDocument
import javax.swing.text.BadLocationException
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.html.HTML
import javax.swing.text.html.HTMLDocument
import javax.swing.text.Element as SwingTextElement

class SwingToolWindow(project: Project) : TaskDescriptionToolWindow(project) {
  override val taskSpecificPanel: JComponent = JPanel(BorderLayout())

  init {
    taskInfoPanel.border = JBUI.Borders.empty(20, 0, 0, 10)
  }

  override fun updateTaskSpecificPanel(task: Task?) {
    taskSpecificPanel.removeAll()
    val panel = createSpecificPanel(task)
    if (panel != null) {
      taskSpecificPanel.add(panel, BorderLayout.CENTER)
      taskSpecificPanel.revalidate()
      taskSpecificPanel.repaint()
    }
  }

  override fun wrapHint(hintElement: Element, displayedHintNumber: String, hintTitle: String): String {
    return wrapHintSwing(project, hintElement, displayedHintNumber, hintTitle)
  }

  class HintElementLinkHandler(project: Project, private val taskInfoTextPane: JTextPane) : SwingToolWindowLinkHandler(project) {
    override fun processEvent(event: HyperlinkEvent) {
      val url = event.description
      if (url.startsWith(HINT_PROTOCOL)) {
        val sourceElement = event.sourceElement
        toggleHintElement(sourceElement)
        return
      }
      super.processEvent(event)
    }

    private fun toggleHintElement(sourceElement: SwingTextElement) {
      try {
        val document = taskInfoTextPane.document as HTMLDocument
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
    private fun changeArrowIcon(sourceElement: SwingTextElement, document: HTMLDocument, iconUrl: String) {
      val resource = javaClass.classLoader.getResource(iconUrl)
      if (resource != null) {
        val arrowImageElement = getArrowImageElement(sourceElement.parentElement)
        document.setOuterHTML(arrowImageElement, String.format("<img src='%s' width='16' height='16'>", resource.toExternalForm()))
      }
      else {
        LOG.warn("Cannot find arrow icon $iconUrl")
      }
    }

    private fun getArrowImageElement(element: SwingTextElement): SwingTextElement? {
      var result: SwingTextElement? = null
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

    private fun getHintTextElement(parent: SwingTextElement): SwingTextElement? {
      var hintTextElement: SwingTextElement? = null
      val children: Enumeration<*> = (parent as AbstractDocument.AbstractElement).parent.children()
      while (children.hasMoreElements()) {
        val child = children.nextElement() as SwingTextElement
        val childAttributes = child.attributes
        val childClassName = childAttributes.getAttribute(HTML.Attribute.CLASS) as String
        if (childClassName == "hint_text") {
          hintTextElement = child
          break
        }
      }
      return hintTextElement
    }
  }

  companion object {
    private val LOG = Logger.getInstance(SwingToolWindow::class.java)

    // all a tagged elements should have different href otherwise they are all underlined on hover. That's why
    // we have to add hint number to href
    private const val HINT_TEXT_PATTERN = "<div class='hint_text'>%s</div>"
  }
}