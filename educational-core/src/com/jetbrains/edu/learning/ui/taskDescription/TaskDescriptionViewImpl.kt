package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.ide.ui.LafManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepikAdaptiveReactionsPanel
import com.jetbrains.edu.learning.ui.taskDescription.check.CheckPanel
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSeparator

class TaskDescriptionViewImpl(val project: Project) : TaskDescriptionView(), DataProvider, Disposable {
  private lateinit var checkPanel: CheckPanel
  private val taskTextTW : TaskDescriptionToolWindow = if (EduUtils.hasJavaFx() && EduSettings.getInstance().shouldUseJavaFx()) JavaFxToolWindow() else SwingToolWindow()
  private val taskTextPanel : JComponent = taskTextTW.createTaskInfoPanel(project)
  private lateinit var separator: JSeparator

  override var currentTask: Task? = null
    set(value) {
      if (currentTask !== null && currentTask === value) return
      setTaskText(value)
      separator.isVisible = value != null
      checkPanel.isVisible = value != null
      if (value != null) {
        readyToCheck()
        checkPanel.updateCheckButton(value)
      }

      taskTextTW.updateTaskSpecificPanel(value)
      UIUtil.setBackgroundRecursively(checkPanel, getTaskDescriptionBackgroundColor())
      field = value
    }
  override fun updateTaskSpecificPanel() {
    taskTextTW.updateTaskSpecificPanel(currentTask)
  }

  override fun updateTaskDescription(task: Task?) {
    setTaskText(task)
    updateTaskSpecificPanel()
  }

  override fun updateTaskDescription() {
    updateTaskDescription(currentTask)
  }

  override fun readyToCheck() {
    checkPanel.readyToCheck()
  }

  override fun init() {
    val panel = JPanel(BorderLayout())
    panel.border = JBUI.Borders.empty(0, 15, 15, 15)
    val course = StudyTaskManager.getInstance(project).course
    if (course != null && course.isAdaptive) {
      panel.add(StepikAdaptiveReactionsPanel(project), BorderLayout.NORTH)
    }

    panel.add(taskTextPanel, BorderLayout.CENTER)
    taskTextPanel.border = JBUI.Borders.empty(0, 0, 10, 0)

    val bottomPanel = JPanel(BorderLayout())
    separator = JSeparator()
    bottomPanel.add(separator, BorderLayout.NORTH)

    val taskSpecificPanel = taskTextTW.createTaskSpecificPanel(currentTask)
    bottomPanel.add(taskSpecificPanel, BorderLayout.CENTER)

    checkPanel = CheckPanel(project)
    checkPanel.border = JBUI.Borders.empty(2, 0, 0, 0)
    bottomPanel.add(checkPanel, BorderLayout.SOUTH)

    panel.add(bottomPanel, BorderLayout.SOUTH)
    UIUtil.setBackgroundRecursively(panel, getTaskDescriptionBackgroundColor())

    setContent(panel)

    project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, EduFileEditorManagerListener(project))
    currentTask = EduUtils.getCurrentTask(project)

    LafManager.getInstance().addLafManagerListener { UIUtil.setBackgroundRecursively(panel, getTaskDescriptionBackgroundColor()) }
  }

  override fun checkStarted() {
    checkPanel.checkStarted()
  }

  override fun checkFinished(task: Task, checkResult: CheckResult) {
    checkPanel.checkFinished(task, checkResult)
    if (checkResult.status == CheckStatus.Failed) {
      updateTaskSpecificPanel()
    }
  }

  override fun dispose() {

  }

  private fun setTaskText(task: Task?) {
    taskTextTW.setTaskText(project, task)
  }
}
