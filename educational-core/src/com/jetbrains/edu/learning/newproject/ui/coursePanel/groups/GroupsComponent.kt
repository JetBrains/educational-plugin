package com.jetbrains.edu.learning.newproject.ui.coursePanel.groups

import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.util.ui.StatusText
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.ErrorState
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView

class GroupsComponent(private val errorHandler: (ErrorState) -> Unit) : JBPanelWithEmptyText(VerticalFlowLayout(0, 0)) {

  private val courseGroupModel: CourseGroupModel = CourseGroupModel()

  val selectedValue: Course?
    get() = courseGroupModel.selectedCard?.courseInfo?.course

  init {
    background = TaskDescriptionView.getTaskDescriptionBackgroundColor()
    emptyText.text = EduCoreBundle.message("course.dialog.no.courses.found")
    emptyText.appendSecondaryText(EduCoreBundle.message("course.dialog.no.courses.found.secondary.text"), StatusText.DEFAULT_ATTRIBUTES,
                                  null)
  }

  fun addGroup(coursesGroup: CoursesGroup) {
    val groupPanel = CoursesGroupPanel(coursesGroup, errorHandler)
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

  fun initialSelection() {
    courseGroupModel.initialSelection()
  }

  fun setSelectionListener(processSelectionChanged: () -> Unit) {
    courseGroupModel.setSelectionListener(processSelectionChanged)
  }

  fun setButtonsEnabled(canStartCourse: Boolean) {
    courseGroupModel.setButtonsEnabled(canStartCourse)
  }
}