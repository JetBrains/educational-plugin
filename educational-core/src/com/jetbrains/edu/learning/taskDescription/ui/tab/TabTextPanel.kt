package com.jetbrains.edu.learning.taskDescription.ui.tab

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.taskDescription.ui.htmlWithResources
import com.jetbrains.edu.learning.taskDescription.ui.wrapHintTagsInsideHTML
import org.jsoup.nodes.Element
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel


abstract class TabTextPanel(val project: Project, val plainText: Boolean = true) : JPanel(BorderLayout()), Disposable {
  abstract val component: JComponent

  init {
    background = TaskDescriptionView.getTaskDescriptionBackgroundColor()
    border = JBUI.Borders.empty(15, 15, 0, 0)
  }

  abstract fun setText(text: String)

  fun setTabText(text: String) {
    val textToSet = if (plainText) {
      text
    }
    else {
      htmlWithResources(project, wrapHints(text))
    }
    setText(textToSet)
  }

  protected abstract fun wrapHint(hintElement: Element, displayedHintNumber: String, hintTitle: String): String

  override fun dispose() {}

  private fun wrapHints(text: String): String {
    return wrapHintTagsInsideHTML(text, this::wrapHint)
  }
}