package com.jetbrains.edu.learning.newproject.ui.coursePanel.groups

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import com.jetbrains.edu.learning.newproject.ui.ErrorState
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import java.awt.BorderLayout
import javax.swing.JPanel


class CoursesListPanel(errorHandler: (ErrorState) -> Unit) : JPanel(BorderLayout()) {
  private val groupsComponent: GroupsComponent = GroupsComponent(errorHandler)
  val selectedCourse: Course? get() = groupsComponent.selectedValue

  init {
    background = MAIN_BG_COLOR
    add(groupsComponent, BorderLayout.CENTER)
  }

  fun updateModel(courseInfos: List<CourseInfo>, courseToSelect: Course?) {
    val sortedCourseInfos = sortCourses(courseInfos)
    val group = CoursesGroup("", sortedCourseInfos)
    addGroup(group)  // TODO: use actual groups

    if (courseToSelect == null) {
      initialSelection()
      return
    }

    val newCourseToSelect = sortedCourseInfos.firstOrNull { courseInfo: CourseInfo -> courseInfo.course == courseToSelect }?.course
    setSelectedValue(newCourseToSelect)
  }

  private fun sortCourses(courseInfos: List<CourseInfo>): List<CourseInfo> {
    val comparator = Comparator
      .comparingInt { courseInfo: CourseInfo -> if (courseInfo.course is JetBrainsAcademyCourse) 0 else 1 }
      .thenComparing { courseInfo: CourseInfo -> courseInfo.course.visibility }
      .thenComparing { courseInfo: CourseInfo -> courseInfo.course.name }

    return courseInfos.sortedWith(comparator)
  }

  private fun addGroup(coursesGroup: CoursesGroup) {
    groupsComponent.addGroup(coursesGroup)
  }

  fun clear() {
    groupsComponent.clear()
  }

  private fun setSelectedValue(newCourseToSelect: Course?) {
    groupsComponent.setSelectedValue(newCourseToSelect)
  }

  fun initialSelection() {
    groupsComponent.initialSelection()
  }

  fun setSelectionListener(processSelectionChanged: () -> Unit) {
    groupsComponent.setSelectionListener(processSelectionChanged)
  }
}