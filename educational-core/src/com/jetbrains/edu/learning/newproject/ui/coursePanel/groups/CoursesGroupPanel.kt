package com.jetbrains.edu.learning.newproject.ui.coursePanel.groups

import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.newproject.ui.CourseCardComponent
import com.jetbrains.edu.learning.newproject.ui.ErrorState
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import java.awt.Color
import javax.swing.JLabel
import javax.swing.JPanel

val SECTION_HEADER_FOREGROUND: Color = JBColor(0x787878, 0x999999)
val SECTION_HEADER_BACKGROUND: Color = JBColor(0xF7F7F7, 0x3C3F41)
const val TOP_BOTTOM = 4
const val LEFT_RIGHT = 10

class CoursesGroupPanel(coursesGroup: CoursesGroup, errorHandler: (ErrorState) -> Unit) : JPanel(VerticalFlowLayout(0, 0)) {

  init {
    background = MAIN_BG_COLOR
    val name = coursesGroup.name

    val titleLabel = JLabel(name)
    titleLabel.isOpaque = true
    titleLabel.toolTipText = name
    titleLabel.foreground = SECTION_HEADER_FOREGROUND
    titleLabel.background = SECTION_HEADER_BACKGROUND
    titleLabel.border = JBUI.Borders.empty(TOP_BOTTOM, LEFT_RIGHT)
    titleLabel.isVisible = name.isNotEmpty()

    add(titleLabel)

    for (courseInfo in coursesGroup.courseInfos) {
      val courseCardComponent = CourseCardComponent(courseInfo, errorHandler)
      courseCardComponent.updateColors(false)
      add(courseCardComponent)
    }
  }

  val courseCards: List<CourseCardComponent>
    get() = components.filterIsInstance<CourseCardComponent>()
}