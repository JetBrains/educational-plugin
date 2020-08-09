package com.jetbrains.edu.learning.newproject.ui.coursePanel.groups

import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.newproject.ui.CourseCardComponent
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseMode
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import java.awt.Color
import javax.swing.JLabel
import javax.swing.JPanel

val SECTION_HEADER_FOREGROUND: Color = JBColor(0x787878, 0x999999)
val SECTION_HEADER_BACKGROUND: Color = JBColor(0xF7F7F7, 0x3C3F41)
const val TOP_BOTTOM = 4
const val LEFT_RIGHT = 10

class CoursesGroupPanel(titleString: String,
                        courseInfos: List<CourseInfo>,
                        joinCourse: (CourseInfo, CourseMode) -> Unit,
                        updateModel: (courseCardComponent: CourseCardComponent) -> Unit) : JPanel(VerticalFlowLayout(0, 0)) {
  init {
    background = TaskDescriptionView.getTaskDescriptionBackgroundColor()

    val titleLabel = JLabel(titleString)
    titleLabel.isOpaque = true
    titleLabel.toolTipText = titleString
    titleLabel.foreground = SECTION_HEADER_FOREGROUND
    titleLabel.background = SECTION_HEADER_BACKGROUND
    titleLabel.border = JBUI.Borders.empty(TOP_BOTTOM, LEFT_RIGHT)

    add(titleLabel)

    for (courseInfo in courseInfos) {
      val courseCardComponent = CourseCardComponent(courseInfo, joinCourse)
      updateModel(courseCardComponent)
      courseCardComponent.updateColors(false)
      add(courseCardComponent)
    }
  }
}