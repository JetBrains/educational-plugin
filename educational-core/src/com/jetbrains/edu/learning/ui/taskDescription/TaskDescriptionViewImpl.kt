package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.ide.ui.LafManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.ui.taskDescription.check.CheckPanel
import java.awt.BorderLayout
import java.awt.Point
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSeparator

class TaskDescriptionViewImpl(val project: Project) : TaskDescriptionView(), DataProvider, Disposable {
  private lateinit var checkPanel: CheckPanel
  private val taskTextTW : TaskDescriptionToolWindow = if (EduUtils.hasJavaFx() && EduSettings.getInstance().shouldUseJavaFx()) JavaFxToolWindow() else SwingToolWindow()
  private val taskTextPanel : JComponent = taskTextTW.createTaskInfoPanel(project)
  private lateinit var separator: JSeparator
  private lateinit var contentManager: ContentManager

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
      updateAdditionalTaskTab(value)

      UIUtil.setBackgroundRecursively(checkPanel, getTaskDescriptionBackgroundColor())
      field = value
    }

  override fun updateAdditionalTaskTab() {
    updateAdditionalTaskTab(currentTask)
  }

  private fun updateAdditionalTaskTab(value: Task?) {
    val additionalTab = StudyTaskManager.getInstance(project).course?.configurator?.additionalTaskTab(value, project)
    if (additionalTab != null) {
      val content = contentManager.findContent(additionalTab.second)
      content?.let { contentManager.removeContent(it, true) }
      val topicsContent = ContentFactory.SERVICE.getInstance().createContent(additionalTab.first, additionalTab.second, false)
      contentManager.addContent(topicsContent, 1)
    }
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

  override fun init(toolWindow: ToolWindow) {
    contentManager = toolWindow.contentManager
    val panel = JPanel(BorderLayout())
    panel.border = JBUI.Borders.empty(0, 15, 15, 0)

    panel.add(taskTextPanel, BorderLayout.CENTER)
    taskTextPanel.border = JBUI.Borders.empty(0, 0, 10, 0)

    val bottomPanel = JPanel(BorderLayout())

    val separatorPanel = JPanel(BorderLayout())
    separatorPanel.border = JBUI.Borders.emptyRight(15)
    separator = JSeparator()
    separatorPanel.add(separator, BorderLayout.CENTER)
    bottomPanel.add(separatorPanel, BorderLayout.NORTH)

    val taskSpecificPanel = taskTextTW.createTaskSpecificPanel(currentTask)
    taskSpecificPanel.border = JBUI.Borders.emptyRight(15)
    bottomPanel.add(taskSpecificPanel, BorderLayout.CENTER)

    checkPanel = CheckPanel(project)
    checkPanel.border = JBUI.Borders.empty(2, 0, 0, 15)
    bottomPanel.add(checkPanel, BorderLayout.SOUTH)

    panel.add(bottomPanel, BorderLayout.SOUTH)
    UIUtil.setBackgroundRecursively(panel, getTaskDescriptionBackgroundColor())
    project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, EduFileEditorManagerListener(project))
    val content = ContentFactory.SERVICE.getInstance().createContent(panel, "Description", false)
    contentManager.addContent(content)
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

  override fun showBalloon(text: String, messageType: MessageType) {
    val balloonBuilder = JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(text, messageType, BrowserHyperlinkListener.INSTANCE)
    val balloon = balloonBuilder.createBalloon()
    val point = Point(checkPanel.x + checkPanel.width - 10, checkPanel.y + checkPanel.height)
    balloon.show(RelativePoint (checkPanel, point), Balloon.Position.above)
  }

  override fun dispose() {
  }

  private fun setTaskText(task: Task?) {
    taskTextTW.setTaskText(project, task)
  }

  override fun getData(dataId: String): Any? {
    return if (PlatformDataKeys.HELP_ID.`is`(dataId)) {
      HELP_ID
    }
    else super.getData(dataId)
  }

  companion object {
    private const val HELP_ID = "task.description"
  }
}
