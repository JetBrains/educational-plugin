package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.yaml.addTabToTaskDescription
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.JavaUILibrary.JCEF
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.getTopPanelForProblem
import com.jetbrains.edu.learning.stepik.hyperskill.metrics.HyperskillMetricsService
import com.jetbrains.edu.learning.stepik.submissions.SubmissionsManager
import com.jetbrains.edu.learning.stepik.submissions.SubmissionsTabPanel
import com.jetbrains.edu.learning.taskDescription.ui.check.CheckPanel
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JSeparator

class TaskDescriptionViewImpl(val project: Project) : TaskDescriptionView(), DataProvider {
  private var uiContent: UiContent? = null

  override var currentTask: Task? = null
    // TODO: move it in some separate method
    set(value) {
      if (currentTask !== null && currentTask === value) return
      val ui = uiContent
      if (ui != null) {
        setTaskText(value)
        ui.separator.isVisible = value != null
        ui.checkPanel.isVisible = value != null
        updateCheckPanel(value)
        updateTopPanel(value)
        ui.taskTextTW.updateTaskSpecificPanel(value)
        updateAdditionalTaskTabs(value)
        HyperskillMetricsService.getInstance().viewEvent(value)
      }
      field = value
    }

  override fun updateAdditionalTaskTabs() {
    updateAdditionalTaskTabs(currentTask)
  }

  private fun updateAdditionalTaskTabs(task: Task?) {
    val contentManager = uiContent?.contentManager ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return
    val additionalTab = course.configurator?.additionalTaskTab(task, project)
    if (additionalTab != null) {
      addTab(contentManager, additionalTab, 1)
    }
    if (task != null && task.supportSubmissions() && SubmissionsManager.getInstance(project).submissionsSupported()) {
      val submissionsTab = SubmissionsTabPanel(project, course, task)
      val submissionsTabIndex = if (additionalTab != null) 2 else getSubmissionsTabIndex(contentManager)
      updateSubmissionsTab(contentManager, submissionsTab, submissionsTabIndex)
    }
    else {
      removeSubmissionsContent(contentManager)
    }

    addYamlTab()
  }

  override fun updateSubmissionsTab() {
    updateSubmissionsTab(currentTask)
  }

  private fun updateSubmissionsTab(task: Task?) {
    if (task == null) return
    val contentManager = uiContent?.contentManager ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return
    if (task.supportSubmissions() && SubmissionsManager.getInstance(project).submissionsSupported()) {
      val submissionsTab = SubmissionsTabPanel(project, course, task)
      updateSubmissionsTab(contentManager, submissionsTab, getSubmissionsTabIndex(contentManager))
    }
    else {
      removeSubmissionsContent(contentManager)
    }
  }

  override fun updateAdditionalTab() {
    val contentManager = uiContent?.contentManager ?: return
    val additionalTab = StudyTaskManager.getInstance(project).course?.configurator?.additionalTaskTab(currentTask, project)
    if (additionalTab != null) {
      addTab(contentManager, additionalTab, 1)
    }
  }

  private fun updateSubmissionsTab(contentManager: ContentManager, submissionsTab: SubmissionsTabPanel?, tabIndex: Int) {
    if (submissionsTab == null) {
      removeSubmissionsContent(contentManager)
    }
    else {
      addTab(contentManager, submissionsTab, tabIndex)
    }
  }

  private fun getSubmissionsTabIndex(contentManager: ContentManager): Int {
    val contents = contentManager.contents.filter { it.tabName != EduCoreBundle.message("submissions.tab.name") }
    return contents.size
  }

  private fun removeSubmissionsContent(contentManager: ContentManager) {
    val contents = contentManager.contents
    val submissionsContent = contents.find { it.tabName == EduCoreBundle.message("submissions.tab.name") }
    if (submissionsContent != null) {
      contentManager.removeContent(submissionsContent, true)
    }
  }

  private fun addTab(contentManager: ContentManager,
                     additionalTab: AdditionalTabPanel,
                     tabIndex: Int) {
    val currentContent = contentManager.selectedContent
    val isAdditionalTabSelected = currentContent?.let { contentManager.getIndexOfContent(it) } == tabIndex
    val content = contentManager.findContent(additionalTab.name)
    content?.let { contentManager.removeContent(it, true) }
    val additionalTabContent = ContentFactory.SERVICE.getInstance().createContent(additionalTab, additionalTab.name, false)
    additionalTabContent.isCloseable = false
    contentManager.addContent(additionalTabContent, tabIndex)
    if (isAdditionalTabSelected) {
      contentManager.setSelectedContent(additionalTabContent)
    }
  }

  private fun addYamlTab() {
    val course = StudyTaskManager.getInstance(project).course ?: return
    if (!course.isStudy) {
      addTabToTaskDescription(project)
    }
  }

  override fun addLoadingPanel(platformName: String) {
    val contentManager = uiContent?.contentManager ?: return
    val submissionsContent = contentManager.findContent(EduCoreBundle.message("submissions.tab.name"))
    if (submissionsContent != null) {
      val submissionsPanel = submissionsContent.component
      if (submissionsPanel is SubmissionsTabPanel) {
        ApplicationManager.getApplication().invokeLater { submissionsPanel.addLoadingPanel(platformName) }
      }
    }
  }

  override fun updateCheckPanel(task: Task?) {
    if (task == null) return
    val checkPanel = uiContent?.checkPanel ?: return
    readyToCheck()
    checkPanel.updateCheckPanel(task)
    UIUtil.setBackgroundRecursively(checkPanel, getTaskDescriptionBackgroundColor())
  }

  override fun updateTaskSpecificPanel() {
    uiContent?.taskTextTW?.updateTaskSpecificPanel(currentTask)
  }

  override fun updateTopPanel(task: Task?) {
    val topPanel = uiContent?.topPanel ?: return
    topPanel.removeAll()
    val course = StudyTaskManager.getInstance(project).course
    if (course is HyperskillCourse) {
      val panel = getTopPanelForProblem(project, course, task) ?: return
      topPanel.add(panel, BorderLayout.SOUTH)
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
    uiContent?.checkPanel?.readyToCheck()
  }

  override fun init(toolWindow: ToolWindow) {
    val contentManager = toolWindow.contentManager
    val panel = JPanel(BorderLayout())
    panel.border = JBUI.Borders.empty(0, 15, 15, 0)

    val taskTextTW = if (EduSettings.getInstance().javaUiLibraryWithCheck == JCEF) JCEFToolWindow(project) else SwingToolWindow(project)
    Disposer.register(contentManager, taskTextTW)

    val taskTextPanel = taskTextTW.createTaskInfoPanel()
    val topPanel = JPanel(BorderLayout())

    panel.add(topPanel, BorderLayout.NORTH)
    panel.add(taskTextPanel, BorderLayout.CENTER)
    taskTextPanel.border = JBUI.Borders.emptyBottom(10)

    val bottomPanel = JPanel(BorderLayout())

    val separatorPanel = JPanel(BorderLayout())
    separatorPanel.border = JBUI.Borders.emptyRight(15)
    val separator = JSeparator()
    separatorPanel.add(separator, BorderLayout.CENTER)
    bottomPanel.add(separatorPanel, BorderLayout.NORTH)

    val taskSpecificPanel = taskTextTW.createTaskSpecificPanel()
    taskSpecificPanel.border = JBUI.Borders.emptyRight(15)
    bottomPanel.add(taskSpecificPanel, BorderLayout.CENTER)

    val checkPanel = CheckPanel(project, contentManager)
    checkPanel.border = JBUI.Borders.empty(2, 0, 0, 15)
    bottomPanel.add(checkPanel, BorderLayout.SOUTH)

    panel.add(bottomPanel, BorderLayout.SOUTH)
    UIUtil.setBackgroundRecursively(panel, getTaskDescriptionBackgroundColor())

    uiContent = UiContent(contentManager, topPanel, taskTextTW, checkPanel, separator)

    val content = ContentFactory.SERVICE.getInstance()
      .createContent(panel, EduCoreBundle.message("label.description"), false)
      .apply { isCloseable = false }
    contentManager.addContent(content)

    currentTask = EduUtils.getCurrentTask(project)
    updateAdditionalTaskTabs(currentTask)

    val connection = project.messageBus.connect()
    connection.subscribe(LafManagerListener.TOPIC, LafManagerListener {
      UIUtil.setBackgroundRecursively(panel, getTaskDescriptionBackgroundColor())
    })
    connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, EduFileEditorManagerListener(project))
  }

  override fun checkStarted(task: Task) {
    if (task != currentTask) return
    uiContent?.checkPanel?.checkStarted()
  }

  override fun checkFinished(task: Task, checkResult: CheckResult) {
    if (task != currentTask) return
    uiContent?.checkPanel?.updateCheckDetails(task, checkResult)
    if (checkResult.status == CheckStatus.Failed) {
      updateTaskSpecificPanel()
    }
  }

  override fun checkTooltipPosition(): RelativePoint? {
    return uiContent?.checkPanel?.checkTooltipPosition()
  }

  private fun setTaskText(task: Task?) {
    uiContent?.taskTextTW?.setTaskText(project, task)
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

  private class UiContent(
    val contentManager: ContentManager,
    val topPanel: JPanel,
    val taskTextTW: TaskDescriptionToolWindow,
    val checkPanel: CheckPanel,
    val separator: JSeparator
  )
}
