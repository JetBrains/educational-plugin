package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
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


class TaskDescriptionView(val project: Project) : SimpleToolWindowPanel(true, true), DataProvider, Disposable {
  private lateinit var checkPanel: CheckPanel
  private lateinit var taskTextTW : TaskDescriptionToolWindow
  private lateinit var taskTextPanel : JComponent
  private lateinit var separator: SeparatorComponent
  var currentTask: Task? = null
    set(value) {
      if (currentTask !== null && currentTask === value) return
      setTaskText(value)
      separator.isVisible = value != null
      checkPanel.isVisible = value != null
      if (value != null) {
        readyToCheck()
      }

      taskTextTW.updateTaskSpecificPanel(value)
      UIUtil.setBackgroundRecursively(checkPanel, EditorColorsManager.getInstance().globalScheme.defaultBackground)
      field = value
    }

  fun updateTaskSpecificPanel() {
    taskTextTW.updateTaskSpecificPanel(currentTask)
  }

  fun updateTaskDescription(task: Task?) {
    setTaskText(task)
    updateTaskSpecificPanel()
  }

  fun updateTaskDescription() {
    updateTaskDescription(currentTask)
  }

  fun readyToCheck() {
    checkPanel.readyToCheck()
  }

  fun init() {
    val panel = JPanel(BorderLayout())

    val course = StudyTaskManager.getInstance(project).course
    if (course != null && course.isAdaptive) {
      panel.add(StepikAdaptiveReactionsPanel(project), BorderLayout.NORTH)
    }

    taskTextTW = if (EduUtils.hasJavaFx() && EduSettings.getInstance().shouldUseJavaFx()) JavaFxToolWindow() else SwingToolWindow()
    taskTextPanel = taskTextTW.createTaskInfoPanel(project)
    panel.add(taskTextPanel, BorderLayout.CENTER)


    separator = SeparatorComponent(10, 15)

    val bottomPanel = JPanel(BorderLayout())
    bottomPanel.border = JBUI.Borders.empty(0, 15, 15, 15)
    bottomPanel.add(separator, BorderLayout.CENTER)
    checkPanel = CheckPanel()
    bottomPanel.add(checkPanel, BorderLayout.SOUTH)

    panel.add(bottomPanel, BorderLayout.SOUTH)
    UIUtil.setBackgroundRecursively(panel, EditorColorsManager.getInstance().globalScheme.defaultBackground)

    setContent(panel)

    project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, EduFileEditorManagerListener(project))
    currentTask = EduUtils.getCurrentTask(project)
  }

  fun checkStarted() {
    checkPanel.checkStarted()
  }

  fun checkFinished(checkResult: CheckResult) {
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