package com.jetbrains.edu.learning.newproject.ui.coursePanel.groups

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.CourseCardComponent
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import java.awt.BorderLayout
import javax.swing.JPanel


class CoursesListPanel(createCourseCard: (Course) -> CourseCardComponent, resetFilters: () -> Unit) : JPanel(BorderLayout()) {
  private val groupsComponent: GroupsComponent = GroupsComponent(createCourseCard, resetFilters)
  val selectedCourse: Course? get() = groupsComponent.selectedValue

  init {
    background = MAIN_BG_COLOR
    add(groupsComponent, BorderLayout.CENTER)
  }

  fun updateModel(coursesGroups: List<CoursesGroup>, courseToSelect: Course?) {
    clear()

    if (coursesGroups.isEmpty()) {
      return
    }

    coursesGroups.forEach { coursesGroup ->
      if (coursesGroup.courses.isNotEmpty()) {
        addGroup(coursesGroup)
      }
    }

    if (courseToSelect == null) {
      initialSelection()
      return
    }

    val newCourseToSelect = coursesGroups.flatMap { it.courses }.firstOrNull { course: Course -> course == courseToSelect }
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

  fun setClickListener(onClick: (Course) -> Boolean) {
    groupsComponent.setClickListener(onClick)
  }

}