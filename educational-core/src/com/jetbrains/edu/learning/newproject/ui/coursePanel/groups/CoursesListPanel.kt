package com.jetbrains.edu.learning.newproject.ui.coursePanel.groups

import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseMode
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import java.awt.BorderLayout
import javax.swing.JPanel

private const val PANEL_WIDTH = 300
private const val PANEL_HEIGHT = 680

class CoursesListPanel(
  selectionChanged: () -> Unit,
  joinCourse: (CourseInfo, CourseMode) -> Unit,
  private val coursesPanel: CoursesPanel
) : JPanel(BorderLayout()) {
  private val groupsComponent: GroupsComponent = GroupsComponent(selectionChanged, joinCourse)
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
}