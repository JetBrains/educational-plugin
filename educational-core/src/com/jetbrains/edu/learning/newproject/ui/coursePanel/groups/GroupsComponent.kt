package com.jetbrains.edu.learning.newproject.ui.coursePanel.groups

import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.components.JBPanelWithEmptyText
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView

class GroupsComponent(selectionChanged: () -> Unit) : JBPanelWithEmptyText(VerticalFlowLayout(0, 0)) {

  private val courseGroupModel: CourseGroupModel = CourseGroupModel(selectionChanged)

  val selectedValue: Course?
    get() = courseGroupModel.selectedCard?.courseInfo?.course

  init {
    background = TaskDescriptionView.getTaskDescriptionBackgroundColor()
    withEmptyText(NO_COURSES)
  }

  fun addGroup(coursesGroup: CoursesGroup) {
    val groupPanel = CoursesGroupPanel(coursesGroup)
    groupPanel.courseCards.forEach { courseGroupModel.addCourseCard(it) }
    add(groupPanel)
  }

  fun clear() {
    courseGroupModel.clear()
    removeAll()
  }

  fun setSelectedValue(newCourseToSelect: Course?) {
    courseGroupModel.setSelection(newCourseToSelect)
  }

  fun initialSelection() {
    courseGroupModel.initialSelection()
  }

  companion object {
    private const val NO_COURSES = "No courses found"
  }
}