package com.jetbrains.edu.learning.taskDescription.ui.tab

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * Tab initialization is made in [com.jetbrains.edu.learning.taskDescription.ui.tab.AdditionalTab.init] method
 * and it must be called in constructor to initialize all necessary UI.
 */
abstract class AdditionalTab(val project: Project, val tabType: TabType) : JPanel(BorderLayout()), Disposable {
  protected abstract val plainText: Boolean
  protected lateinit var innerTextPanel: TabTextPanel
  val content: Content by lazy { createContent() }

  abstract fun update(task: Task)

  override fun dispose() {}

  protected fun init() {
    innerTextPanel = createTextPanel()
    add(innerTextPanel, BorderLayout.CENTER)
    Disposer.register(this, innerTextPanel)
  }

  protected open fun createTextPanel(): TabTextPanel {
    return if (EduSettings.getInstance().javaUiLibraryWithCheck == JavaUILibrary.JCEF) {
      JCEFTextPanel(project, plainText)
    }
    else {
      SwingTextPanel(project)
    }
  }

  /**
   * @param text text to be inserted to panel
   */
  protected fun setText(text: String) {
    if (!this::innerTextPanel.isInitialized) {
      error("setText must be called after init() method")
    }
    innerTextPanel.setTabText(text)
  }

  private fun createContent(): Content {
    val tabName = tabType.tabName
    return ContentFactory.getInstance().createContent(this, tabName, false)
  }
}
