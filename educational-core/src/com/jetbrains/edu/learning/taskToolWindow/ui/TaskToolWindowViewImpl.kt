package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduExperimentalFeatures.USE_NAVIGATION_MAP
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.marketplace.isMarketplaceCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.hyperskill.metrics.HyperskillMetricsService
import com.jetbrains.edu.learning.submissions.SubmissionsTab
import com.jetbrains.edu.learning.taskToolWindow.ui.navigationMap.NavigationMapAction
import com.jetbrains.edu.learning.taskToolWindow.ui.navigationMap.NavigationMapPanel
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabManager
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType.SUBMISSIONS_TAB
import java.awt.Component
import javax.swing.*

class TaskToolWindowViewImpl(project: Project) : TaskToolWindowView(project), DataProvider {
  private var myTaskPanelHeader: TaskPanelHeader? = null
  private lateinit var tabManager: TabManager

  override var currentTask: Task? = null
    // TODO: move it in some separate method
    set(value) {
      if (currentTask !== null && currentTask === value) return
      val ui = myTaskPanelHeader
      if (ui != null) {
        tabManager.updateTaskDescription(value)
        tabManager.descriptionTab.isSeparatorVisible = value != null
        tabManager.descriptionTab.isCheckPanelVisible = value != null
        updateCheckPanel(value)
        updateNavigationPanel(value)
        myTaskPanelHeader?.taskName?.text = value?.presentableName ?: EduCoreBundle.message("item.task.title")
        myTaskPanelHeader?.lessonHeader?.setHeader(value?.lesson?.name)
        updateTabs(value)
        HyperskillMetricsService.getInstance().viewEvent(value)
        EduCounterUsageCollector.viewEvent(value)
      }
      field = value
    }

  private var newNav = isFeatureEnabled(USE_NAVIGATION_MAP) || project.course is HyperskillCourse

  override fun updateTabs(task: Task?) {
    val taskToUpdate = task ?: currentTask
    tabManager.updateTabs(taskToUpdate)
  }

  override fun updateTab(tabType: TabType) {
    val task = currentTask ?: return
    tabManager.updateTab(tabType, task)
  }

  override fun selectTab(tabType: TabType) {
    tabManager.selectTab(tabType)
  }

  override fun showLoadingSubmissionsPanel(platformName: String) {
    if (currentTask == null) return
    val submissionsTab = tabManager.getTab(SUBMISSIONS_TAB) as SubmissionsTab
    ApplicationManager.getApplication().invokeLater {
      submissionsTab.showLoadingPanel(platformName)
    }
  }

  override fun showLoadingCommunityPanel(platformName: String) {
    if (currentTask == null || !project.isMarketplaceCourse()) return

    val submissionsTab = tabManager.getTab(SUBMISSIONS_TAB) as SubmissionsTab
    ApplicationManager.getApplication().invokeLater {
      submissionsTab.showLoadingCommunityPanel(platformName)
    }
  }

  override fun updateCheckPanel(task: Task?) {
    if (task == null) return
    readyToCheck()
    tabManager.descriptionTab.updateCheckPanel(task)
  }

  override fun updateTaskSpecificPanel() {
    tabManager.descriptionTab.updateTaskSpecificPanel(currentTask)
  }

  override fun updateNavigationPanel(task: Task?) {
    task ?: return

    if (newNav) {
      val navigationPanel = myTaskPanelHeader?.navigationMapPanel ?: return
      navigationPanel as NavigationMapPanel
      myTaskPanelHeader?.lessonHeader?.setHeader(task.lesson.presentableName)

      var index = 1
      val actions = task.lesson.taskList.map { NavigationMapAction(it, task, if (it is TheoryTask) index else index++) }
      navigationPanel.replaceActions(actions)

      val course = StudyTaskManager.getInstance(project).course
      if (course is HyperskillCourse) {
        myTaskPanelHeader?.lessonHeader?.updateTopPanelForProblems(project, course, task)
      }
    }
    else {
      val course = StudyTaskManager.getInstance(project).course
      if (course is HyperskillCourse) {
        myTaskPanelHeader?.lessonHeader?.updateTopPanelForProblems(project, course, task)
      }
    }
  }

  override fun updateNavigationPanel() = updateNavigationPanel(currentTask)

  override fun updateTaskDescription(task: Task?) {
    tabManager.updateTaskDescription(task)
  }

  override fun updateTaskDescription() {
    updateTaskDescription(currentTask)
    updateCheckPanel(currentTask)
  }

  override fun readyToCheck() {
    tabManager.descriptionTab.readyToCheck()
  }

  override fun init(toolWindow: ToolWindow) {
    tabManager = TabManager(project)

    //create main panel
    val mainPanel = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
      border = JBUI.Borders.empty(0, 16, 12, 16)
    }

    //create lesson header
    val lessonHeader = LessonHeader()
    mainPanel.add(lessonHeader)

    //create and add navigationPanel if needed
    val navigationPanel = if (newNav) {
      val navigationMapPanel = NavigationMapPanel()
      mainPanel.add(navigationMapPanel)
      navigationMapPanel
    }
    else {
      null
    }

    //create task name
    val taskNameBox = Box.createHorizontalBox()
    val taskName = JLabel(EduCoreBundle.message("item.task.title"))
    taskName.font = JBFont.h1()
    taskName.alignmentX = Component.LEFT_ALIGNMENT
    taskNameBox.add(taskName)
    taskNameBox.add(Box.createHorizontalGlue())
    taskNameBox.border = JBEmptyBorder(0,0,4,0)

    mainPanel.add(taskNameBox)
    mainPanel.add(tabManager.tabbedPane)

    UIUtil.setBackgroundRecursively(mainPanel, getTaskDescriptionBackgroundColor())

    myTaskPanelHeader = TaskPanelHeader(lessonHeader, navigationPanel, taskName/*, taskTextTW, checkPanel, separator*/)

    val content = ContentFactory.getInstance()
      .createContent(mainPanel, EduCoreBundle.message("item.task.title"), false)
      .apply { isCloseable = false }
    toolWindow.contentManager.addContent(content)

    currentTask = project.getCurrentTask()
    updateTabs(currentTask)

    val connection = project.messageBus.connect()
    connection.subscribe(LafManagerListener.TOPIC, LafManagerListener {
      UIUtil.setBackgroundRecursively(mainPanel, getTaskDescriptionBackgroundColor())
    })
  }

  override fun checkStarted(task: Task, startSpinner: Boolean) {
    if (task != currentTask) return
    tabManager.descriptionTab.checkStarted(startSpinner)
  }

  override fun checkFinished(task: Task, checkResult: CheckResult) {
    if (task != currentTask) return
    tabManager.descriptionTab.updateCheckDetails(task, checkResult)
    if (task is DataTask || task.isChangedOnFailed) {
      updateCheckPanel(task)
    }
    if (checkResult.status == CheckStatus.Failed) {
      tabManager.updateTaskSpecificPanel(task)
    }
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

  class TaskPanelHeader(
    val lessonHeader: LessonHeader,
    val navigationMapPanel: JPanel?,
    val taskName: JLabel
  )
}
