package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.ide.ui.LafManager
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.yaml.addTabToTaskDescription
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.configuration.EduConfiguratorWithSubmissions.Companion.SUBMISSIONS_TAB_NAME
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.getTopPanelForProblem
import com.jetbrains.edu.learning.taskDescription.ui.check.CheckPanel
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSeparator

class TaskDescriptionViewImpl(val project: Project) : TaskDescriptionView(), DataProvider {
  private lateinit var checkPanel: CheckPanel
  private val taskTextTW : TaskDescriptionToolWindow = if (EduUtils.hasJavaFx() && EduSettings.getInstance().shouldUseJavaFx()) JavaFxToolWindow() else SwingToolWindow()
  private val taskTextPanel : JComponent = taskTextTW.createTaskInfoPanel(project)
  private val topPanel: JPanel = JPanel(BorderLayout())
  private lateinit var separator: JSeparator
  private lateinit var contentManager: ContentManager

  override var currentTask: Task? = null
    set(value) {
      if (currentTask !== null && currentTask === value) return
      setTaskText(value)
      separator.isVisible = value != null
      checkPanel.isVisible = value != null
      updateCheckPanel(value)
      updateTopPanel(value)
      taskTextTW.updateTaskSpecificPanel(value)
      updateAdditionalTaskTab(value)
      field = value
    }

  override fun updateAdditionalTaskTab() {
    updateAdditionalTaskTab(currentTask)
  }

  private fun updateAdditionalTaskTab(task: Task?) {
    val additionalTab = StudyTaskManager.getInstance(project).course?.configurator?.additionalTaskTab(task, project)
    if (additionalTab != null) {
      val currentContent = contentManager.selectedContent
      val isAdditionalTabSelected = currentContent?.let { contentManager.getIndexOfContent(it) } == 1
      val content = contentManager.findContent(additionalTab.second)
      content?.let { contentManager.removeContent(it, true) }
      val topicsContent = ContentFactory.SERVICE.getInstance().createContent(additionalTab.first, additionalTab.second, false)
      topicsContent.isCloseable = false
      contentManager.addContent(topicsContent, 1)
      if (isAdditionalTabSelected) {
        contentManager.setSelectedContent(topicsContent)
      }
    }
    else {
      val contents = contentManager.contents
      val submissionsContent = contents.find { it.tabName == SUBMISSIONS_TAB_NAME }
      if (submissionsContent != null) contentManager.removeContent(submissionsContent, true)
    }

    addYamlTab()
  }

  private fun addYamlTab() {
    val course = StudyTaskManager.getInstance(project).course ?: return
    if (!course.isStudy) {
      addTabToTaskDescription(project)
    }
  }

  private fun updateCheckPanel(task: Task?) {
    if (task == null) return
    readyToCheck()
    checkPanel.updateCheckPanel(task)
    UIUtil.setBackgroundRecursively(checkPanel, getTaskDescriptionBackgroundColor())
  }

  override fun updateTaskSpecificPanel() {
    taskTextTW.updateTaskSpecificPanel(currentTask)
  }

  override fun updateTopPanel(task: Task?) {
    topPanel.removeAll()
    val course = StudyTaskManager.getInstance(project).course
    if (course is HyperskillCourse) {
      val panel = getTopPanelForProblem(project, course, task) ?: return
      topPanel.add(panel, BorderLayout.CENTER)
      topPanel.add(JSeparator(), BorderLayout.SOUTH)
    }
  }

  override fun updateTaskDescription(task: Task?) {
    setTaskText(task)
    updateTaskSpecificPanel()
  }

  override fun updateTaskDescription() {
    updateTaskDescription(currentTask)
    updateCheckPanel(currentTask)
  }

  override fun readyToCheck() {
    checkPanel.readyToCheck()
  }

  override fun init(toolWindow: ToolWindow) {
    contentManager = toolWindow.contentManager
    val panel = JPanel(BorderLayout())
    panel.border = JBUI.Borders.empty(0, 15, 15, 0)

    panel.add(topPanel, BorderLayout.NORTH)
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
    val course = StudyTaskManager.getInstance(project).course
    val displayNamePrefix = if (course is HyperskillCourse) "Stage" else "Task"
    val content = ContentFactory.SERVICE.getInstance().createContent(panel, "$displayNamePrefix Description", false)
    content.isCloseable = false
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

  override fun checkTooltipPosition(): RelativePoint {
    return checkPanel.checkTooltipPosition()
  }

  private fun setTaskText(task: Task?) {
    taskTextTW.setTaskText(project, task)
  }

  override fun getData(dataId: String): Any? {
    return if (PlatformDataKeys.HELP_ID.`is`(dataId)) {
      HELP_ID
    }
    else null
  }

  companion object {
    private const val HELP_ID = "task.description"
  }
}
