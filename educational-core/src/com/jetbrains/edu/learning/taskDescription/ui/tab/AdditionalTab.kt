package com.jetbrains.edu.learning.taskDescription.ui.tab

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Tab initialization is made in [com.jetbrains.edu.learning.taskDescription.ui.tab.AdditionalTab.setupTextViewer] method,
 * and it must be called in constructor to initialize all necessary UI.
 */
abstract class AdditionalTab(val project: Project, val tabType: TabType) : JPanel(BorderLayout()), Disposable {

  protected abstract val innerTextPanel: JComponent
  val content: Content by lazy { createContent() }

  abstract fun update(task: Task)

  override fun dispose() {}

  /**
   * This function must be called in the constructors of all subclasses
   */
  protected fun setupTextViewer() {
    add(innerTextPanel, BorderLayout.CENTER)
    innerTextPanel.background = TaskDescriptionView.getTaskDescriptionBackgroundColor()
    innerTextPanel.border = JBUI.Borders.empty(15, 15, 0, 0)
  }

  private fun createContent(): Content {
    val tabName = tabType.tabName
    return ContentFactory.getInstance().createContent(this, tabName, false)
  }
}
