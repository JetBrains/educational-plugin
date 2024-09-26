package com.jetbrains.edu.learning.taskToolWindow.ui.tab

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBEmptyBorder
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.awt.BorderLayout
import javax.swing.JPanel

abstract class TaskToolWindowTab(val project: Project, private val tabType: TabType) : JPanel(BorderLayout()), Disposable {
  protected open val uiMode: JavaUILibrary = EduSettings.getInstance().javaUiLibrary

  init {
    border = JBEmptyBorder(0)
  }

  abstract fun update(task: Task)

  override fun dispose() {}
}
