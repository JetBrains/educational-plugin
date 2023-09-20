package com.jetbrains.edu.learning.taskToolWindow.ui.tab

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.awt.BorderLayout
import javax.swing.JPanel

abstract class AdditionalTab(val project: Project, val tabType: TabType) : JPanel(BorderLayout()), Disposable {
  val content: Content by lazy { createContent() }
  protected open val uiMode: JavaUILibrary = EduSettings.getInstance().javaUiLibraryWithCheck

  abstract fun update(task: Task)

  override fun dispose() {}

  private fun createContent(): Content {
    val tabName = tabType.tabName
    return ContentFactory.getInstance().createContent(this, tabName, false)
  }
}
