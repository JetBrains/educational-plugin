package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduExperimentalFeatures.USE_NAVIGATION_MAP
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.JavaUILibrary.JCEF
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.hyperskill.getTopPanelForProblem
import com.jetbrains.edu.learning.stepik.hyperskill.metrics.HyperskillMetricsService
import com.jetbrains.edu.learning.submissions.SubmissionsTab
import com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckPanel
import com.jetbrains.edu.learning.taskToolWindow.ui.navigationMap.NavigationMapAction
import com.jetbrains.edu.learning.taskToolWindow.ui.navigationMap.NavigationMapPanel
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabManager
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabManagerImpl
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType.SUBMISSIONS_TAB
import java.awt.BorderLayout
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.swing.JPanel
import javax.swing.JSeparator

class TaskToolWindowViewImpl(project: Project) : TaskToolWindowView(project), DataProvider {
  private var uiContent: UiContent? = null
  private lateinit var tabManager: TabManager

  private val newNav = isFeatureEnabled(USE_NAVIGATION_MAP)

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
        updateNavigationPanel(value)
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

  override fun showLoadingSubmissionsPanel(platformName: String) {
    if (currentTask == null) return
    val submissionsTab = tabManager.getTab(SUBMISSIONS_TAB) as SubmissionsTab
    ApplicationManager.getApplication().invokeLater {
      submissionsTab.showLoadingPanel(platformName)
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

  override fun updateNavigationPanel(task: Task?) {
    task ?: return

    val navigationPanel = uiContent?.navigationMapPanel ?: return
    if (newNav) {
      navigationPanel as NavigationMapPanel
      navigationPanel.setHeader(task.lesson.presentableName)
      var index = 1
      val actions = task.lesson.taskList.map { NavigationMapAction(it, task, if (it is TheoryTask) index else index++) }
      navigationPanel.replaceActions(actions)

      val course = StudyTaskManager.getInstance(project).course
      if (course is HyperskillCourse) {
        navigationPanel.updateTopPanelForProblems(project, course, task)
      }
    }
    else {
      navigationPanel.removeAll()
      val course = StudyTaskManager.getInstance(project).course
      if (course is HyperskillCourse) {
        val panel = getTopPanelForProblem(project, course, task) ?: return
        navigationPanel.add(panel, BorderLayout.SOUTH)
      }
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
    val navigationPanel = if (newNav) NavigationMapPanel() else JPanel(BorderLayout())

    panel.add(navigationPanel, BorderLayout.NORTH)
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

    uiContent = UiContent(navigationPanel, taskTextTW, checkPanel, separator)

    val content = ContentFactory.getInstance()
      .createContent(panel, EduCoreBundle.message("label.description"), false)
      .apply { isCloseable = false }
    contentManager.addContent(content)

    currentTask = project.getCurrentTask()
    updateAdditionalTaskTabs(currentTask)

    val connection = project.messageBus.connect()
    connection.subscribe(LafManagerListener.TOPIC, LafManagerListener {
      UIUtil.setBackgroundRecursively(panel, getTaskDescriptionBackgroundColor())
    })
  }

  override fun checkStarted(task: Task, startSpinner: Boolean) {
    if (task != currentTask) return
    uiContent?.checkPanel?.checkStarted(startSpinner)
  }

  override fun checkFinished(task: Task, checkResult: CheckResult) {
    if (task != currentTask) return
    uiContent?.checkPanel?.updateCheckDetails(task, checkResult)
    if (task is DataTask || task.isChangedOnFailed) {
      updateCheckPanel(task)
    }
    if (checkResult.status == CheckStatus.Failed) {
      updateTaskSpecificPanel()
    }
    if (checkResult == CheckResult.SOLVED) {
      val taskId = 1 + task.course.allTasks.indexOf(task)
      val deploymentId = 10
      val contextId = 2
      val userId = 3

      val url = URL("https://amazing-wildcat-on.ngrok-free.app/open_in_ide/post_scores/$taskId/$deploymentId/$contextId/$userId")
      val con = url.openConnection() as HttpsURLConnection
      con.setRequestMethod("GET")
      val responseCode = con.responseCode
      logger<TaskToolWindowViewImpl>().info("LTI for task $taskId got response $responseCode")
      con.disconnect()
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
    val navigationMapPanel: JPanel,
    val taskTextTW: TaskToolWindow,
    val checkPanel: CheckPanel,
    val separator: JSeparator
  )
}
