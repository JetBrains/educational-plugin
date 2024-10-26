package com.jetbrains.edu.learning.newproject.ui.coursePanel.groups

import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBPanelWithEmptyText
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.CourseCardComponent
import com.jetbrains.edu.learning.newproject.ui.coursePanel.SelectCourseBackgroundColor

class GroupsComponent(
  private val createCourseCard: (Course) -> CourseCardComponent,
  resetFilters: () -> Unit
) : JBPanelWithEmptyText(VerticalFlowLayout(0, 0)) {
  private val courseGroupModel: CourseGroupModel = CourseGroupModel()

  val selectedValue: Course?
    get() = courseGroupModel.selectedCard?.course

  init {
    background = SelectCourseBackgroundColor
    emptyText.text = EduCoreBundle.message("course.dialog.no.courses.found")
    emptyText.appendSecondaryText(
      EduCoreBundle.message("course.dialog.no.courses.found.secondary.text"),
      SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES
    ) { resetFilters() }
  }

  fun addGroup(coursesGroup: CoursesGroup) {
    val groupPanel = CoursesGroupPanel(coursesGroup, createCourseCard)
    groupPanel.courseCards.forEach { courseGroupModel.addCourseCard(it) }
    add(groupPanel)
  }

  fun clear() {
    courseGroupModel.clear()
    removeAll()
    revalidate()
    repaint()
  }

  fun setSelectedValue(newCourseToSelect: Course?) {
    courseGroupModel.setSelection(newCourseToSelect)
  }

  fun removeSelection() {
    courseGroupModel.removeSelection()
  }

  fun initialSelection() {
    courseGroupModel.initialSelection()
  }

  fun setSelectionListener(processSelectionChanged: () -> Unit) {
    courseGroupModel.onSelection = processSelectionChanged
  }
}
