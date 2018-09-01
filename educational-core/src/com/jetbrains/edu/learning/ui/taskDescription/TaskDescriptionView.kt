package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.SeparatorComponent
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.ui.taskDescription.check.CheckPanel
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel


class TaskDescriptionView(val project: Project) : SimpleToolWindowPanel(true, true), DataProvider, Disposable {
  override fun dispose() {

  }

  private lateinit var checkPanel: CheckPanel

  fun init() {
    val panel = JPanel()
    panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

    panel.addWithLeftAlignment(JBScrollPane(JBLabel("text placeholder")))
    panel.addWithLeftAlignment(SeparatorComponent(10, 15))

    val bottomPanel = JPanel(BorderLayout())
    bottomPanel.border = JBUI.Borders.empty(0, 15, 15, 15)
    checkPanel = CheckPanel()
    bottomPanel.add(checkPanel, BorderLayout.NORTH)
    panel.addWithLeftAlignment(bottomPanel)

    UIUtil.setBackgroundRecursively(panel, EditorColorsManager.getInstance().globalScheme.defaultBackground)

    setContent(panel)
  }

  fun checkStarted() {
    checkPanel.checkStarted()
  }

  fun checkFinished(checkResult: CheckResult) {
    checkPanel.checkFinished(checkResult)
  }

  private fun JPanel.addWithLeftAlignment(component: JComponent) {
    add(component)
    component.alignmentX = Component.LEFT_ALIGNMENT
  }

  companion object {

    @JvmStatic
    fun getInstance(project: Project): TaskDescriptionView {
      if (!EduUtils.isStudyProject(project)) {
        error("Attempt to get TaskDescriptionView for non-edu project")
      }
      return ServiceManager.getService(project, TaskDescriptionView::class.java)
    }
  }
}