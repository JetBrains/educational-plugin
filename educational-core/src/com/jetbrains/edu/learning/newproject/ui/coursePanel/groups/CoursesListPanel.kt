package com.jetbrains.edu.learning.newproject.ui.coursePanel.groups

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseMode
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import javax.swing.JPanel

private const val PANEL_WIDTH = 300
private const val PANEL_HEIGHT = 680

class CoursesListPanel(
  selectionChanged: () -> Unit,
  joinCourse: (CourseInfo, CourseMode) -> Unit,
  private val coursesProvider: CoursesPlatformProvider,
  private val coursesPanel: CoursesPanel
) : JPanel(BorderLayout()) {
  private val groupsComponent: GroupsComponent = GroupsComponent(selectionChanged, joinCourse)
  private var busConnection: MessageBusConnection? = null
  private val panelSize = JBUI.size(PANEL_WIDTH, PANEL_HEIGHT)

  val selectedCourse: Course? get() = groupsComponent.selectedValue

  init {
    background = MAIN_BG_COLOR
    preferredSize = panelSize
    maximumSize = panelSize
    minimumSize = panelSize
    add(groupsComponent, BorderLayout.CENTER)
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