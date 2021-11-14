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
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.JavaUILibrary.JCEF
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.codeforces.CodeforcesSettings
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.getTopPanelForProblem
import com.jetbrains.edu.learning.stepik.hyperskill.metrics.HyperskillMetricsService
import com.jetbrains.edu.learning.taskDescription.ui.check.CheckPanel
import com.jetbrains.edu.learning.taskDescription.ui.tab.SwingTextPanel
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabManager
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabManagerImpl
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabType
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabType.SUBMISSIONS_TAB
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JSeparator

class TaskDescriptionViewImpl(val project: Project) : TaskDescriptionView(), DataProvider {
  private var uiContent: UiContent? = null
  private lateinit var tabManager: TabManager

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
        EduCounterUsageCollector.viewEvent(value)
      }
      field = value
    }

  override fun updateAdditionalTaskTabs(task: Task?) {
    val taskToUpdate = task ?: currentTask
    tabManager.updateTabs(taskToUpdate)
  }

  override fun updateTab(tabType: TabType) {
    tabManager.updateTab(tabType, currentTask)
  }

  override fun showTab(tabType: TabType) {
    tabManager.selectTab(tabType)
  }

  override fun addLoadingPanel(platformName: String) {
    if (currentTask == null) return
    val submissionsContent = tabManager.getTab(SUBMISSIONS_TAB).content
    val panel = submissionsContent.component
    if (panel is SwingTextPanel) {
      ApplicationManager.getApplication().invokeLater { panel.addLoadingSubmissionsPanel(platformName) }
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
    tabManager = TabManagerImpl(project, contentManager)
    Disposer.register(contentManager, tabManager)

    val panel = JPanel(BorderLayout())
    panel.border = JBUI.Borders.empty(0, 15, 15, 0)

    val taskTextTW = if (EduSettings.getInstance().javaUiLibraryWithCheck == JCEF) JCEFToolWindow(project) else SwingToolWindow(project)
    Disposer.register(contentManager, taskTextTW)

    val taskTextPanel = taskTextTW.taskInfoPanel
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

    val taskSpecificPanel = taskTextTW.taskSpecificPanel
    taskSpecificPanel.border = JBUI.Borders.emptyRight(15)
    bottomPanel.add(taskSpecificPanel, BorderLayout.CENTER)

    val checkPanel = CheckPanel(project, contentManager)
    checkPanel.border = JBUI.Borders.empty(2, 0, 0, 15)
    bottomPanel.add(checkPanel, BorderLayout.SOUTH)

    panel.add(bottomPanel, BorderLayout.SOUTH)
    UIUtil.setBackgroundRecursively(panel, getTaskDescriptionBackgroundColor())

    uiContent = UiContent(topPanel, taskTextTW, checkPanel, separator)

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
    connection.subscribe(CodeforcesSettings.AUTHENTICATION_TOPIC, object : EduLogInListener {
      override fun userLoggedIn() {
        val task = EduUtils.getCurrentTask(project)
        if (task != null) {
          ApplicationManager.getApplication().invokeLater { checkPanel.updateCheckPanel(task) }
        }
      }

      override fun userLoggedOut() {
        val task = EduUtils.getCurrentTask(project)
        if (task != null) {
          ApplicationManager.getApplication().invokeLater { checkPanel.updateCheckPanel(task) }
        }
      }
    })
  }

  override fun checkStarted(task: Task, startSpinner: Boolean) {
    if (task != currentTask) return
    uiContent?.checkPanel?.checkStarted(startSpinner)
  }

  override fun checkFinished(task: Task, checkResult: CheckResult) {
    if (task != currentTask) return
    uiContent?.checkPanel?.updateCheckDetails(task, checkResult)
    if (task is DataTask) {
      updateCheckPanel(task)
    }
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

  class UiContent(
    val topPanel: JPanel,
    val taskTextTW: TaskDescriptionToolWindow,
    val checkPanel: CheckPanel,
    val separator: JSeparator
  )
}
