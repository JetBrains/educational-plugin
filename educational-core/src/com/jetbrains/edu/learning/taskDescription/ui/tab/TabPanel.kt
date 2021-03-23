package com.jetbrains.edu.learning.taskDescription.ui.tab

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.taskDescription.ui.htmlWithResources
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabManager.TabType
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.event.HyperlinkListener


abstract class TabPanel(val project: Project, val tabType: TabType) : JPanel(BorderLayout()), Disposable {
  init {
    background = TaskDescriptionView.getTaskDescriptionBackgroundColor()
    border = JBUI.Borders.empty(15, 15, 0, 0)
  }

  protected abstract fun setText(text: String)

  fun setTabText(text: String, plain: Boolean) {
    val textToSet = if (plain) {
      text
    }
    else {
      htmlWithResources(project, wrapHints(text))
    }
    setText(textToSet)
  }

  protected abstract fun wrapHint(hintElement: Element, displayedHintNumber: String): String

  open fun addHyperlinkListener(listener: HyperlinkListener) {}

  override fun dispose() {}

  protected fun wrapHints(text: String): String {
    val document = Jsoup.parse(text)
    val hints = document.getElementsByClass("hint")
    if (hints.size == 1) {
      val hint = hints[0]
      val hintText = wrapHint(hint, "")
      hint.html(hintText)
      return document.html()
    }
    for (i in hints.indices) {
      val hint = hints[i]
      val hintText = wrapHint(hint, (i + 1).toString())
      hint.html(hintText)
    }
    return document.html()
  }

  companion object {
    private const val TAB_PANEL_UPDATE: String = "Tab Panel Update"
  }
}