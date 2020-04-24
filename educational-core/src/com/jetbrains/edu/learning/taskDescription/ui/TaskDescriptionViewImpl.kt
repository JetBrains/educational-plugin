package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import com.intellij.util.containers.addIfNotNull
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.yaml.addTabToTaskDescription
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.JavaUILibrary.*
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.configuration.EduConfiguratorWithSubmissions.Companion.SUBMISSIONS_TAB_NAME
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.getTopPanelForProblem
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
      }
      field = value
    }

  override fun updateAdditionalTaskTabs() {
    updateAdditionalTaskTabs(currentTask)
  }

  private fun updateAdditionalTaskTabs(task: Task?) {
    val contentManager = uiContent?.contentManager ?: return
    val submissionsTab = StudyTaskManager.getInstance(project).course?.configurator?.submissionsTaskTab(task, project)
    val topicsTab = StudyTaskManager.getInstance(project).course?.configurator?.topicsTaskTab(task, project)
//    val topicsTab = if(submissionsTab != null) {Pair(submissionsTab.first, "Topics")}
//      else null
    val tabsList = mutableListOf<Pair<JPanel, String>>()
    tabsList.addIfNotNull(topicsTab)
    tabsList.addIfNotNull(submissionsTab)
    tabsList.sortedByDescending { it.second }
    for (tab in tabsList) {
      addTab(tab, contentManager, tabsList.indexOf(tab) + 1)
    }
    if (tabsList.find { it.second == SUBMISSIONS_TAB_NAME } == null) {
      removeSubmissionsContent(contentManager)
    }

    addYamlTab()
  }

  private fun removeSubmissionsContent(contentManager: ContentManager) {
    val contents = contentManager.contents
    val submissionsContent = contents.find { it.tabName == SUBMISSIONS_TAB_NAME }
    if (submissionsContent != null) {
      contentManager.removeContent(submissionsContent, true)
    }
  }

  private fun addTab(additionalTab: Pair<JPanel, String>, contentManager: ContentManager, placement: Int) {
    val currentContent = contentManager.selectedContent
    val isAdditionalTabSelected = currentContent?.let { contentManager.getIndexOfContent(it) } == placement
    val content = contentManager.findContent(additionalTab.second)
    content?.let { contentManager.removeContent(it, true) }
    val topicsContent = ContentFactory.SERVICE.getInstance().createContent(additionalTab.first, additionalTab.second, false)
    topicsContent.isCloseable = false
    contentManager.addContent(topicsContent, placement)
    if (isAdditionalTabSelected) {
      contentManager.setSelectedContent(topicsContent)
    }
  }

  private fun addYamlTab() {
    val course = StudyTaskManager.getInstance(project).course ?: return
    if (!course.isStudy) {
      addTabToTaskDescription(project)
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
    uiContent?.checkPanel?.readyToCheck()
  }

  override fun init(toolWindow: ToolWindow) {
    val contentManager = toolWindow.contentManager
    val panel = JPanel(BorderLayout())
    panel.border = JBUI.Borders.empty(0, 15, 15, 0)

    val taskTextTW = when (EduSettings.getInstance().javaUiLibraryWithCheck) {
      SWING -> SwingToolWindow(project)
      JAVAFX -> JavaFxToolWindow(project)
      JCEF -> getJCEFToolWindow(project) ?: SwingToolWindow(project)
    }
    val taskTextPanel = taskTextTW.createTaskInfoPanel()
    val topPanel = JPanel(BorderLayout())

    panel.add(topPanel, BorderLayout.NORTH)
    panel.add(taskTextPanel, BorderLayout.CENTER)
    taskTextPanel.border = JBUI.Borders.empty(0, 0, 10, 0)

    val bottomPanel = JPanel(BorderLayout())

    val separatorPanel = JPanel(BorderLayout())
    separatorPanel.border = JBUI.Borders.emptyRight(15)
    val separator = JSeparator()
    separatorPanel.add(separator, BorderLayout.CENTER)
    bottomPanel.add(separatorPanel, BorderLayout.NORTH)

    val taskSpecificPanel = taskTextTW.createTaskSpecificPanel()
    taskSpecificPanel.border = JBUI.Borders.emptyRight(15)
    bottomPanel.add(taskSpecificPanel, BorderLayout.CENTER)

    val checkPanel = CheckPanel(project)
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

    // TODO: provide correct parent disposable here to correctly unload the plugin
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
