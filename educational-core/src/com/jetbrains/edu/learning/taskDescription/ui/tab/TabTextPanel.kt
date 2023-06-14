package com.jetbrains.edu.learning.taskDescription.ui.tab

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel


abstract class TabTextPanel(val project: Project) : JPanel(BorderLayout()), Disposable {
  abstract val component: JComponent

  init {
    background = TaskDescriptionView.getTaskDescriptionBackgroundColor()
    border = JBUI.Borders.empty(15, 15, 0, 0)
  }

  abstract fun setText(text: String)

  override fun dispose() {}
}