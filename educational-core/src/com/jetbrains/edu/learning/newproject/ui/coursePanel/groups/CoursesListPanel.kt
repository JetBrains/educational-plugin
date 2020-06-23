package com.jetbrains.edu.learning.newproject.ui.coursePanel.groups

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseMode
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import java.awt.BorderLayout
import javax.swing.JPanel


class CoursesListPanel(
  selectionChanged: () -> Unit,
  joinCourse: (CourseInfo, CourseMode) -> Unit
) : JPanel(BorderLayout()) {
  private val groupsComponent: GroupsComponent = GroupsComponent(selectionChanged, joinCourse)

  val selectedCourse: Course? get() = groupsComponent.selectedValue

  init {
    background = MAIN_BG_COLOR
    add(groupsComponent, BorderLayout.CENTER)
  }

  fun updateModel(courseInfos: List<CourseInfo>, courseToSelect: Course?) {
    val sortedCourseInfos = sortCourses(courseInfos)
    addGroup("", sortedCourseInfos)  // TODO: use actual groups

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