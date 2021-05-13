package com.jetbrains.edu.learning.taskDescription.ui.tab

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabManager.TabType
import java.awt.BorderLayout
import javax.swing.event.HyperlinkListener

open class AdditionalTab(val project: Project, tabType: TabType) {
  protected val panel = TabPanel(tabType)
  private val textPanel by lazy { createTextPanel() }

  init {
    panel.add(textPanel, BorderLayout.CENTER)
    Disposer.register(panel, textPanel)
  }

  protected open fun createTextPanel(): TabTextPanel {
    return if (EduSettings.getInstance().javaUiLibraryWithCheck == JavaUILibrary.JCEF) {
      JCEFTextPanel(project)
    }
    else {
      SwingTextPanel(project)
    }
  }

  /**
   * @param text text to be inserted to panel
   * @param plain if false, text will proceed through [com.jetbrains.edu.learning.taskDescription.ui.tab.TabTextPanel.wrapHints]
   * and then will be inserted to "/style/template.html.ft" template as a content or text with Html resources and wrapping hints
   */
  protected fun setText(text: String, plain: Boolean) {
    textPanel.setTabText(text, plain)
  }

  protected fun addHyperlinkListener(listener: HyperlinkListener) = textPanel.addHyperlinkListener(listener)

  fun createContent(): Content {
    val tabName = panel.tabType.tabName
    return ContentFactory.SERVICE.getInstance().createContent(panel, tabName, false)
  }
}
