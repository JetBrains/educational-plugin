package com.jetbrains.edu.learning.taskDescription.ui.tab

import com.intellij.openapi.project.Project
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabManager.TabType
import javax.swing.event.HyperlinkListener

open class AdditionalTab(val project: Project, private val tabType: TabType) {
  private val panel: TabPanel by lazy { createTabPanel() }

  protected open fun createTabPanel(): TabPanel {
    return if (EduSettings.getInstance().javaUiLibraryWithCheck == JavaUILibrary.JCEF) {
      JCEFTabPanel(project, tabType)
    }
    else {
      SwingTabPanel(project, tabType)
    }
  }

  /**
   * @param text text to be inserted to panel
   * @param plain if false, text will proceed through [com.jetbrains.edu.learning.taskDescription.ui.tab.TabPanel.wrapHints]
   * and then will be inserted to "/style/template.html.ft" template as a content or text with Html resources and wrapping hints
   */
  protected fun setText(text: String, plain: Boolean) {
    panel.setTabText(text, plain)
  }

  protected fun addHyperlinkListener(listener: HyperlinkListener) = panel.addHyperlinkListener(listener)

  fun createContent(): Content {
    val tabName = tabType.tabName
    return ContentFactory.SERVICE.getInstance().createContent(panel, tabName, false)
  }
}
