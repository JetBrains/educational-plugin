package com.jetbrains.edu.learning.newproject.ui

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.compatibility.CourseCompatibility
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import com.jetbrains.edu.learning.newproject.ui.coursePanel.NewCoursePanel
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*

private const val PANEL_WIDTH = 450
private const val PANEL_HEIGHT = 680

private const val TOOLBAR_TOP_OFFSET = 10
private const val TOOLBAR_BOTTOM_OFFSET = 8
private const val TOOLBAR_LEFT_OFFSET = 13

class CoursesListPanel(
  private val toolbarAction: AnAction?,
  private val coursesProvider: CoursesPlatformProvider,
  private val tabInfo: TabInfo?
) : JPanel(BorderLayout()) {
  private var coursesList: JBList<Course> = JBList()
  private var busConnection: MessageBusConnection? = null
  private var hoveredIndex: Int = -1
  private val panelSize = JBUI.size(PANEL_WIDTH, PANEL_HEIGHT)
  private var tabInfoPanel: TabInfoPanel? = null

  val selectedCourse: Course? get() = coursesList.selectedValue

  init {
    coursesList.setEmptyText(EduCoreBundle.message("course.dialog.no.courses.found"))
    coursesList.cellRenderer = CourseColoredListCellRenderer()
    coursesList.border = null
    coursesList.background = TaskDescriptionView.getTaskDescriptionBackgroundColor()
    coursesList.addMouseMotionListener(CourseMouseMotionListener())
    coursesList.addMouseMotionListener(CourseMouseMotionListener())

    preferredSize = panelSize
    maximumSize = panelSize
    minimumSize = panelSize
    border = JBUI.Borders.customLine(NewCoursePanel.DIVIDER_COLOR, 0, 0, 0, 1)

    add(createListPanel(), BorderLayout.CENTER)
  }

  fun selectFirstCourse() {
    courses.firstOrNull()?.let {
      coursesList.setSelectedValue(it, true)
    }
  }

  val courses: List<Course>
    get() {
      val model = coursesList.model
      val courses = mutableListOf<Course>()
      for (i in 0 until model.size) {
        courses.add(model.getElementAt(i))
      }

      return courses
    }

  suspend fun loadCourses(): List<Course> {
    return withContext(Dispatchers.IO) {
      coursesProvider.loadCourses().filter {
        val compatibility = it.compatibility
        compatibility == CourseCompatibility.Compatible || compatibility is CourseCompatibility.PluginsRequired
      }
    }
  }

  private fun createListPanel(): JPanel {
    val panel = JPanel(BorderLayout())
    val listWithTabInfo = JPanel(BorderLayout())
    if (tabInfo != null) {
      tabInfoPanel = TabInfoPanel(tabInfo).apply {
        listWithTabInfo.add(this, BorderLayout.NORTH)
      }
    }
    listWithTabInfo.add(coursesList, BorderLayout.CENTER)
    val scrollPane = JBScrollPane(listWithTabInfo)
    scrollPane.border = null
    scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
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

    return hyperlinkPanel
  }

  fun updateModel(courses: List<Course>, courseToSelect: Course?) {
    val sortedCourses = sortCourses(courses)
    val listModel = DefaultListModel<Course>()
    for (course in sortedCourses) {
      listModel.addElement(course)
    }
    coursesList.model = listModel
    val courseToShow = courseToSelect ?: sortedCourses.firstOrNull()
    if (courseToShow == null) {
      return
    }
    sortedCourses
      .firstOrNull { course: Course -> course === courseToShow }
      ?.let { coursesList.setSelectedValue(it, true) }
  }

  private fun sortCourses(courses: List<Course>): List<Course> {
    val comparator = Comparator
      .comparingInt { element: Course -> if (element is JetBrainsAcademyCourse) 0 else 1 }
      .thenComparing(Course::getVisibility)
      .thenComparing(Course::getName)

    return courses.sortedWith(comparator)
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

  fun addListener(onSelectionChanged: () -> Unit) {
    coursesList.addListSelectionListener { onSelectionChanged() }
  }

  fun hideLoginPanel() {
    tabInfoPanel?.hideLoginPanel()
  }

  private inner class CourseMouseMotionListener : MouseAdapter() {
    override fun mouseMoved(event: MouseEvent) {
      val hoveredIndex = coursesList.locationToIndex(event.point)
      if (hoveredIndex != hoveredIndex) {
        this@CoursesListPanel.hoveredIndex = hoveredIndex
        coursesList.repaint()
      }
    }

    override fun mouseExited(event: MouseEvent?) {
      if (hoveredIndex != -1) {
        hoveredIndex = -1
        coursesList.repaint()
      }
    }
  }

  private inner class CourseColoredListCellRenderer : ListCellRenderer<Course?> {
    override fun getListCellRendererComponent(list: JList<out Course?>?,
                                              value: Course?,
                                              index: Int,
                                              isSelected: Boolean,
                                              cellHasFocus: Boolean): Component {
      val courseCardComponent = CourseCardComponent(value)
      courseCardComponent.updateColors(isSelected || hoveredIndex == index)
      return courseCardComponent
    }
  }
}
