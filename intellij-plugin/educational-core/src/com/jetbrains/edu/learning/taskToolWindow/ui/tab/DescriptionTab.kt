package com.jetbrains.edu.learning.taskToolWindow.ui.tab

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.taskToolWindow.ui.JCEFToolWindow
import com.jetbrains.edu.learning.taskToolWindow.ui.SolutionSharingInlineBanner
import com.jetbrains.edu.learning.taskToolWindow.ui.SwingToolWindow
import com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckPanel
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JSeparator

/**
 * Constructor is called exclusively in [com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabManager.createTab]
 * and MUST NOT be called in any other places
 */
class DescriptionTab(project: Project) : TaskToolWindowTab(project, TabType.DESCRIPTION_TAB) {

  private val taskTextToolWindow = if (EduSettings.getInstance().javaUiLibraryWithCheck == JavaUILibrary.JCEF) {
    JCEFToolWindow(project)
  }
  else {
    SwingToolWindow(project)
  }
  private val checkPanel = CheckPanel(project, this)
  private val separatorPanel = JPanel(BorderLayout())

  var isSeparatorVisible: Boolean = true
    set(value) {
      separatorPanel.isVisible = value
      field = value
    }

  var isCheckPanelVisible: Boolean = checkPanel.isVisible
    set(value) {
      checkPanel.isVisible = value
      field = value
    }
    get() = checkPanel.isVisible

  init {
    Disposer.register(this, taskTextToolWindow)

    val taskDescription = taskTextToolWindow.taskInfoPanel
    taskDescription.border = JBUI.Borders.emptyBottom(10)

    add(taskDescription, BorderLayout.CENTER)

    val bottomPanel = JPanel(BorderLayout())
    add(bottomPanel, BorderLayout.SOUTH)

    separatorPanel.border = JBUI.Borders.emptyRight(15)
    val separator = JSeparator()
    separatorPanel.add(separator, BorderLayout.CENTER)
    bottomPanel.add(separatorPanel, BorderLayout.NORTH)

    val taskSpecificPanel = taskTextToolWindow.taskSpecificPanel
    taskSpecificPanel.border = JBUI.Borders.emptyRight(15)
    bottomPanel.add(taskSpecificPanel, BorderLayout.CENTER)

    checkPanel.border = JBUI.Borders.empty(2, 0, 0, 10)
    bottomPanel.add(checkPanel, BorderLayout.SOUTH)
  }

  fun updateTaskSpecificPanel(task: Task?) {
    taskTextToolWindow.updateTaskSpecificPanel(task)
  }

  override fun update(task: Task) {
    taskTextToolWindow.setTaskText(project, task)
    taskTextToolWindow.updateTaskSpecificPanel(task)
  }

  fun updateCheckDetails(task: Task, checkResult: CheckResult) = checkPanel.updateCheckDetails(task, checkResult)

  fun updateCheckPanel(task: Task) = checkPanel.updateCheckPanel(task)

  fun readyToCheck() = checkPanel.readyToCheck()

  fun checkStarted(startSpinner: Boolean) = checkPanel.checkStarted(startSpinner)
  
  fun addInlineBanner(inlineBanner: SolutionSharingInlineBanner) {
    val taskInfoPanel = taskTextToolWindow.taskInfoPanel
    taskInfoPanel.add(inlineBanner, BorderLayout.SOUTH)
    taskInfoPanel.validate()
  }

}