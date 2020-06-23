package com.jetbrains.edu.learning.newproject.ui.coursePanel.groups

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.TabInfo
import com.jetbrains.edu.learning.newproject.ui.TabInfoPanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseMode
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import com.jetbrains.edu.learning.newproject.ui.coursePanel.NewCoursePanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

private const val PANEL_WIDTH = 300
private const val PANEL_HEIGHT = 680

private const val TOOLBAR_TOP_OFFSET = 10
private const val TOOLBAR_BOTTOM_OFFSET = 8
private const val TOOLBAR_LEFT_OFFSET = 13

class CoursesListPanel(
  selectionChanged: () -> Unit,
  joinCourse: (CourseInfo, CourseMode) -> Unit,
  private val toolbarAction: AnAction?,
  private val coursesProvider: CoursesPlatformProvider,
  private val tabInfo: TabInfo?,
  private val coursesPanel: CoursesPanel
) : JPanel(BorderLayout()) {
  private val groupsComponent: GroupsComponent = GroupsComponent(selectionChanged, joinCourse)
  private var busConnection: MessageBusConnection? = null
  private val panelSize = JBUI.size(PANEL_WIDTH, PANEL_HEIGHT)
  private var tabInfoPanel: TabInfoPanel? = null

  val selectedCourse: Course? get() = groupsComponent.selectedValue

  init {
    background = MAIN_BG_COLOR
    preferredSize = panelSize
    maximumSize = panelSize
    minimumSize = panelSize
    add(createListPanel(), BorderLayout.CENTER)
  }

  private fun createListPanel(): JPanel {
    val panel = JPanel(BorderLayout())
    val listWithTabInfo = JPanel(BorderLayout())
    if (tabInfo != null) {
      tabInfoPanel = TabInfoPanel(tabInfo).apply {
        listWithTabInfo.add(this, BorderLayout.NORTH)
      }
    }
    listWithTabInfo.add(groupsComponent, BorderLayout.CENTER)

    val scrollPane = JBScrollPane(listWithTabInfo,
                                  ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                  ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
    scrollPane.apply {
      border = null
      background = MAIN_BG_COLOR
    }

    panel.add(scrollPane, BorderLayout.CENTER)
    if (toolbarAction != null) {
      val toolbarPanel = createToolbarPanel(toolbarAction)
      panel.add(toolbarPanel, BorderLayout.SOUTH)
      scrollPane.border = JBUI.Borders.customLine(NewCoursePanel.DIVIDER_COLOR, 0, 0, 1, 0)
    }

    return panel
  }

  private fun createToolbarPanel(toolbarAction: AnAction): JPanel {
    val hyperlinkLabel = HyperlinkLabel(toolbarAction.templateText)
    hyperlinkLabel.addHyperlinkListener {
      val actionEvent = AnActionEvent.createFromAnAction(toolbarAction, null,
                                                         ActionPlaces.UNKNOWN,
                                                         DataManager.getInstance().getDataContext(this))
      toolbarAction.actionPerformed(actionEvent)
    }

    val hyperlinkPanel = JPanel(BorderLayout())
    hyperlinkPanel.border = JBUI.Borders.empty(TOOLBAR_TOP_OFFSET, TOOLBAR_LEFT_OFFSET, TOOLBAR_BOTTOM_OFFSET, 0)
    hyperlinkPanel.add(hyperlinkLabel, BorderLayout.CENTER)
    UIUtil.setBackgroundRecursively(hyperlinkPanel, MAIN_BG_COLOR)

    return hyperlinkPanel
  }

  fun hideLoginPanel() {
    tabInfoPanel?.hideLoginPanel()
  }

  fun updateModel(courses: List<Course>, courseToSelect: Course?) {
    val sortedCourses = sortCourses(courses)
    val sortedCourseInfos = sortedCourses.map { CourseInfo(it, { coursesPanel.locationString }, { coursesPanel.projectSettings }) }
    addGroup("", sortedCourseInfos)  // TODO: use actual groups

    if (courseToSelect == null) {
      initialSelection()
      return
    }

    val newCourseToSelect = courses.first { course: Course -> course == courseToSelect }
    setSelectedValue(newCourseToSelect)
  }

  private fun sortCourses(courses: List<Course>): List<Course> {
    val comparator = Comparator
      .comparingInt { element: Course -> if (element is JetBrainsAcademyCourse) 0 else 1 }
      .thenComparing(Course::getVisibility)
      .thenComparing(Course::getName)

    return courses.sortedWith(comparator)
  }

  fun addLoginListener(vararg postLoginActions: () -> Unit) {
    if (busConnection != null) {
      busConnection!!.disconnect()
    }
    busConnection = ApplicationManager.getApplication().messageBus.connect()
    busConnection!!.subscribe(EduSettings.SETTINGS_CHANGED, object : EduLogInListener {
      override fun userLoggedOut() {}
      override fun userLoggedIn() {
        runPostLoginActions(*postLoginActions)
      }
    })
  }

  private fun runPostLoginActions(vararg postLoginActions: () -> Unit) {
    invokeLater(modalityState = ModalityState.any()) {
      for (action in postLoginActions) {
        action()
      }
      if (busConnection != null) {
        busConnection!!.disconnect()
        busConnection = null
      }
    }
  }

  fun addGroup(titleString: String, courseInfos: List<CourseInfo>) {
    groupsComponent.addGroup(titleString, courseInfos)
  }

  fun clear() {
    groupsComponent.clear()
  }

  fun setSelectedValue(newCourseToSelect: Course?) {
    groupsComponent.setSelectedValue(newCourseToSelect)
  }

  fun initialSelection() {
    groupsComponent.initialSelection()
  }

  suspend fun loadCourses(): List<Course> {
    return withContext(Dispatchers.IO) { coursesProvider.loadCourses() }
  }
}