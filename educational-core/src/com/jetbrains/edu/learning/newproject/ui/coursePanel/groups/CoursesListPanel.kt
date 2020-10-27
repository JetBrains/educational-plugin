package com.jetbrains.edu.learning.newproject.ui.coursePanel.groups

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import java.awt.BorderLayout
import javax.swing.JPanel


class CoursesListPanel(joinCourseAction: (CourseInfo, CourseMode) -> Unit, resetFilters: () -> Unit) : JPanel(BorderLayout()) {
  private val groupsComponent: GroupsComponent = GroupsComponent(joinCourseAction, resetFilters)
  val selectedCourse: Course? get() = groupsComponent.selectedValue

  init {
    background = MAIN_BG_COLOR
    add(groupsComponent, BorderLayout.CENTER)
  }

  fun updateModel(courseInfos: List<CourseInfo>, courseToSelect: Course?) {
    clear()

    if (courseInfos.isEmpty()) {
      return
    }
    val group = CoursesGroup("", courseInfos)
    addGroup(group)  // TODO: use actual groups

    if (courseToSelect == null) {
      initialSelection()
      return
    }

    val newCourseToSelect = courseInfos.firstOrNull { courseInfo: CourseInfo -> courseInfo.course == courseToSelect }?.course
    setSelectedValue(newCourseToSelect)
  }

  private fun addGroup(coursesGroup: CoursesGroup) {
    groupsComponent.addGroup(coursesGroup)
  }

  fun clear() {
    groupsComponent.clear()
  }

  fun setSelectedValue(newCourseToSelect: Course?) {
    groupsComponent.setSelectedValue(newCourseToSelect)
  }

  private fun initialSelection() {
    groupsComponent.initialSelection()
  }

  fun setSelectionListener(processSelectionChanged: () -> Unit) {
    groupsComponent.setSelectionListener(processSelectionChanged)
  }

}