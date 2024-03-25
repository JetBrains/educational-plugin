package com.jetbrains.edu.learning.taskToolWindow.ui.tab

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.edu.learning.JavaUILibrary
import java.awt.BorderLayout

/**
 * Tab initialization is made in [com.jetbrains.edu.learning.taskToolWindow.ui.tab.TaskToolWindowTextTab.init] method
 * and it must be called in constructor to initialize all necessary UI.
 */
abstract class TaskToolWindowTextTab(project: Project, tabType: TabType) : TaskToolWindowTab(project, tabType) {
  private lateinit var innerPanel: TabTextPanel

  private fun createTextPanel(): TabTextPanel {
    return if (uiMode == JavaUILibrary.JCEF) {
      JCEFTextPanel(project)
    }
    else {
      SwingTextPanel(project)
    }
  }

  protected fun init() {
    innerPanel = createTextPanel()
    add(innerPanel, BorderLayout.CENTER)
    Disposer.register(this, innerPanel)
  }

  /**
   * @param text text to be inserted to panel
   */
  protected fun setText(text: String) {
    if (!this::innerPanel.isInitialized) {
      error("setText must be called after init() method")
    }
    innerPanel.setText(text)
  }
}