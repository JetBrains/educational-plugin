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

/**
 * Tab initialization is made in [com.jetbrains.edu.learning.taskDescription.ui.tab.AdditionalTab.init] method
 * and it must be called in constructor to initialize all necessary UI.
 */
open class AdditionalTab(val project: Project, val tabType: TabType) : JPanel(BorderLayout()), Disposable {
  private lateinit var innerTextPanel: TabTextPanel

  override fun dispose() {}

  protected fun init() {
    innerTextPanel = createTextPanel()
    add(innerTextPanel, BorderLayout.CENTER)
    Disposer.register(this, innerTextPanel)
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
   *
   * Method must be called after [com.jetbrains.edu.learning.taskDescription.ui.tab.AdditionalTab.init]
   */
  protected fun setText(text: String, plain: Boolean) {
    if (!this::innerTextPanel.isInitialized) {
      error("setText must be called after init() method")
    }
    innerTextPanel.setTabText(text, plain)
  }

  fun createContent(): Content {
    val tabName = tabType.tabName
    return ContentFactory.SERVICE.getInstance().createContent(this, tabName, false)
  }
}
