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
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.jsoup.nodes.Element
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.text.html.HTMLEditorKit

class SwingToolWindow(project: Project) : TaskDescriptionToolWindow(project) {
  private val taskInfoTextPane: JTextPane
  override val taskInfoPanel: JComponent = JPanel(BorderLayout())
  override val taskSpecificPanel: JComponent = JPanel(BorderLayout())

  init {
    // we are using HTMLEditorKit here because otherwise styles are not applied
    val editorKit = HTMLEditorKit()
    editorKit.styleSheet = null
    taskInfoTextPane = createTextPane(editorKit)
    val scrollPane = JBScrollPane(taskInfoTextPane)
    scrollPane.border = null
    taskInfoPanel.add(scrollPane, BorderLayout.CENTER)
    taskInfoTextPane.border = JBUI.Borders.empty(20, 0, 0, 10)
    val toolWindowLinkHandler = SwingToolWindowLinkHandler(project, taskInfoTextPane)
    taskInfoTextPane.addHyperlinkListener(toolWindowLinkHandler.hyperlinkListener)
  }

  override fun updateTaskSpecificPanel(task: Task?) {
    taskSpecificPanel.removeAll()
    val panel = task.createSpecificPanel()
    if (panel != null) {
      taskSpecificPanel.add(panel, BorderLayout.CENTER)
      taskSpecificPanel.revalidate()
      taskSpecificPanel.repaint()
    }
  }

  public override fun setText(text: String, task: Task?) {
    taskInfoTextPane.text = htmlWithResources(project, wrapHints(text, task))
  }

  override fun wrapHint(hintElement: Element, displayedHintNumber: String): String {
    return wrapHint(project, hintElement, displayedHintNumber)
  }

  companion object {
    private val LOG = Logger.getInstance(SwingToolWindow::class.java)

    // all a tagged elements should have different href otherwise they are all underlined on hover. That's why
    // we have to add hint number to href
    private const val HINT_BLOCK_TEMPLATE = "  <img src='%s' width='16' height='16' >" +
                                            "  <span><a href='hint://%s', value='%s'>Hint %s</a>" +
                                            "  <img src='%s' width='16' height='16' >"
    private const val HINT_EXPANDED_BLOCK_TEMPLATE = "  <img src='%s' width='16' height='16' >" +
                                                     "  <span><a href='hint://%s', value='%s'>Hint %s</a>" +
                                                     "  <img src='%s' width='16' height='16' >" +
                                                     "  <div class='hint_text'>%s</div>"

    fun wrapHint(project: Project,
                 hintElement: Element,
                 displayedHintNumber: String): String {
      val bulbIcon = getIconFullPath("style/hint/swing/swing_icons/retina_bulb.png", "/style/hint/swing/swing_icons/bulb.png")
      val hintText = hintElement.html()
      if (displayedHintNumber.isEmpty() || displayedHintNumber == "1") {
        hintElement.wrap("<div class='top'></div>")
      }
      val course = StudyTaskManager.getInstance(project).course
      return if (course != null && !course.isStudy) {
        val downIcon = getIconFullPath("/style/hint/swing/swing_icons/retina_down.png",
                                       "/style/hint/swing/swing_icons/down.png")
        String.format(HINT_EXPANDED_BLOCK_TEMPLATE, bulbIcon, displayedHintNumber, hintText,
                      displayedHintNumber, downIcon, hintText)
      }
      else {
        val leftIcon = getIconFullPath("/style/hint/swing/swing_icons/retina_right.png",
                                       "/style/hint/swing/swing_icons/right.png")
        String.format(HINT_BLOCK_TEMPLATE, bulbIcon, displayedHintNumber, hintText, displayedHintNumber,
                      leftIcon)
      }
    }

    private fun getIconFullPath(retinaPath: String, path: String): String {
      val bulbPath = if (UIUtil.isRetina()) retinaPath else path
      val bulbIconUrl = SwingToolWindow::class.java.classLoader.getResource(bulbPath)
      if (bulbIconUrl == null) {
        LOG.warn("Cannot find bulb icon")
      }
      return if (bulbIconUrl == null) "" else bulbIconUrl.toExternalForm()
    }
  }
}