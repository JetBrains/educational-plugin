package com.jetbrains.edu.learning.taskToolWindow.ui.tab

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.JPanel

abstract class TaskToolWindowCardTextTab(project: Project, tabType: TabType) : TaskToolWindowTab(project, tabType) {

  private val cardLayout = CardLayout()

  private val cardLayoutPanel = object : JPanel(cardLayout), Disposable {
    override fun dispose() {}
  }

  protected val headerPanel = JPanel(BorderLayout()).apply {
    border = JBUI.Borders.empty(16, 16, 16, 0)
    isVisible = false
  }

  init {
    this.add(headerPanel, BorderLayout.NORTH)
    List(2) { SwingTextPanel(project) }.forEach {
      cardLayoutPanel.add(it)
      Disposer.register(cardLayoutPanel, it)
    }
    this.add(cardLayoutPanel, BorderLayout.CENTER)
  }

  protected fun cards(): Array<in TabTextPanel> = cardLayoutPanel.components

  protected fun showFirstCard() = cardLayout.first(cardLayoutPanel)

  protected fun showLastCard() = cardLayout.last(cardLayoutPanel)
}
