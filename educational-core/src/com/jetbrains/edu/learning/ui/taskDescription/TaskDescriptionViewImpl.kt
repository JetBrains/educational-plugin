package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.ui.SeparatorComponent
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

class TaskDescriptionViewImpl(val project: Project) : TaskDescriptionView(), DataProvider, Disposable {
  private lateinit var checkPanel: CheckPanel
  private val taskTextTW : TaskDescriptionToolWindow = if (EduUtils.hasJavaFx() && EduSettings.getInstance().shouldUseJavaFx()) JavaFxToolWindow() else SwingToolWindow()
  private val taskTextPanel : JComponent = taskTextTW.createTaskInfoPanel(project)
  private lateinit var separator: SeparatorComponent

  override var currentTask: Task? = null
    set(value) {
      if (currentTask !== null && currentTask === value) return
      setTaskText(value)
      separator.isVisible = value != null
      checkPanel.isVisible = value != null
      if (value != null) {
        readyToCheck()
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
    taskTextPanel.border = JBUI.Borders.empty(0, 0, 8, 0)

    val bottomPanel = JPanel(BorderLayout())
    separator = SeparatorComponent(2, 0)
    bottomPanel.add(separator, BorderLayout.NORTH)

    val taskSpecificPanel = taskTextTW.createTaskSpecificPanel(currentTask)
    bottomPanel.add(taskSpecificPanel, BorderLayout.CENTER)

    checkPanel = CheckPanel(project)
    bottomPanel.add(checkPanel, BorderLayout.SOUTH)

    panel.add(bottomPanel, BorderLayout.SOUTH)
    UIUtil.setBackgroundRecursively(panel, getTaskDescriptionBackgroundColor())

    setContent(panel)

    project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, EduFileEditorManagerListener(project))
    currentTask = EduUtils.getCurrentTask(project)
  }

  override fun checkStarted() {
    checkPanel.checkStarted()
  }

  override fun checkFinished(checkResult: CheckResult) {
    checkPanel.checkFinished(checkResult)
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
