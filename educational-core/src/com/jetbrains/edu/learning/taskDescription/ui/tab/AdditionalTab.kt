package com.jetbrains.edu.learning.taskDescription.ui.tab

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabManager.TabType
import java.awt.BorderLayout
import javax.swing.JPanel

open class AdditionalTab(val project: Project, val tabType: TabType) : JPanel(BorderLayout()), Disposable {
  private val innerTextPanel by lazy { createTextPanel() }

  override fun dispose() {}

  private fun createTextPanel(): TabTextPanel {
    val panel = getTextPanel()
    add(panel, BorderLayout.CENTER)
    Disposer.register(this, panel)
    return panel
  }

  protected open fun getTextPanel(): TabTextPanel {
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
    innerTextPanel.setTabText(text, plain)
  }

  fun createContent(): Content {
    val tabName = tabType.tabName
    return ContentFactory.SERVICE.getInstance().createContent(this, tabName, false)
  }
}
