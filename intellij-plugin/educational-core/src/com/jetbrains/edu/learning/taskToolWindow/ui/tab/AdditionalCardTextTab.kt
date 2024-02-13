package com.jetbrains.edu.learning.taskToolWindow.ui.tab

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.JPanel

abstract class AdditionalCardTextTab(project: Project, tabType: TabType) : AdditionalTab(project, tabType) {

  private val cardLayout = CardLayout()

  private val cardLayoutPanel = JPanel(cardLayout)

  protected val headerPanel = JPanel(BorderLayout()).apply {
    border = JBUI.Borders.empty(16, 16, 16, 0)
    isVisible = false
  }

  init {
    this.add(headerPanel, BorderLayout.NORTH)
    repeat(2)  {
      val swingTextPanel = SwingTextPanel(project)
      cardLayoutPanel.add(swingTextPanel)
      Disposer.register(this, swingTextPanel)
    }
    this.add(cardLayoutPanel, BorderLayout.CENTER)
  }

  protected fun cards(): Array<in TabTextPanel> = cardLayoutPanel.components

  protected fun showFirstCard() = cardLayout.first(cardLayoutPanel)

  protected fun showLastCard() = cardLayout.last(cardLayoutPanel)
}
