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
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.ui.taskDescription.check.CheckPanel
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.BoxLayout
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
      UIUtil.setBackgroundRecursively(checkPanel, EditorColorsManager.getInstance().globalScheme.defaultBackground)
      field = value
    }

  private fun readyToCheck() {
    checkPanel.readyToCheck()
  }

  fun init() {
    val panel = JPanel()
    panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

    taskTextTW = if (EduUtils.hasJavaFx() && EduSettings.getInstance().shouldUseJavaFx()) JavaFxToolWindow() else SwingToolWindow()
    taskTextPanel = taskTextTW.createTaskInfoPanel(project)
    panel.addWithLeftAlignment(taskTextPanel)

    separator = SeparatorComponent(10, 15)
    panel.addWithLeftAlignment(separator)

    val bottomPanel = JPanel(BorderLayout())
    bottomPanel.border = JBUI.Borders.empty(0, 15, 15, 15)
    checkPanel = CheckPanel()
    bottomPanel.add(checkPanel, BorderLayout.NORTH)
    panel.addWithLeftAlignment(bottomPanel)

    UIUtil.setBackgroundRecursively(panel, EditorColorsManager.getInstance().globalScheme.defaultBackground)

    setContent(panel)

    project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER,
                                           EduFileEditorManagerListener(project))
    currentTask = EduUtils.getCurrentTask(project)
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