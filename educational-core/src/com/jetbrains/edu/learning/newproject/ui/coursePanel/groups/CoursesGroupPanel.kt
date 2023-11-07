package com.jetbrains.edu.learning.newproject.ui.coursePanel.groups

import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.coursesList.CourseCardComponent
import com.jetbrains.edu.learning.newproject.ui.coursePanel.SelectCourseBackgroundColor
import com.jetbrains.edu.learning.newproject.ui.getColorFromScheme
import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.SwingConstants


private val SECTION_HEADER_FOREGROUND: Color = JBColor.namedColor(
  "BrowseCourses.GroupHeader.foreground",
  getColorFromScheme("Plugins.SectionHeader.foreground", JBColor(0x787878, 0x999999))
)

private val SECTION_HEADER_BACKGROUND: Color = JBColor.namedColor(
  "BrowseCourses.GroupHeader.background",
  getColorFromScheme("Plugins.SectionHeader.background", JBColor(0xF7F7F7, 0x3C3F41))
)

private const val TOP_BOTTOM = 4
private const val LEFT_RIGHT = 10
private const val COLLAPSIBLE_GROUP_SIZE = 5

class CoursesGroupPanel(coursesGroup: CoursesGroup, createCourseCard: (Course) -> CourseCardComponent) : JPanel(VerticalFlowLayout(0, 0)) {
  private val courseCardsPanel = NonOpaquePanel(VerticalFlowLayout(0, 0))

  init {
    background = SelectCourseBackgroundColor
    val name = coursesGroup.name
    val isCollapsible = coursesGroup.courses.size > COLLAPSIBLE_GROUP_SIZE
    add(createTitleLabel(name, isCollapsible))

    fillCourseCardsPanel(coursesGroup, createCourseCard)
    add(courseCardsPanel)
  }

  private fun createTitleLabel(name: String, isCollapsible: Boolean): JBLabel {
    val titleLabel = JBLabel(name).apply {
      if (isCollapsible) {
        icon = AllIcons.General.ArrowDown
      }
      horizontalTextPosition = SwingConstants.TRAILING
      isOpaque = true
      toolTipText = name
      foreground = SECTION_HEADER_FOREGROUND
      background = SECTION_HEADER_BACKGROUND
      border = JBUI.Borders.empty(TOP_BOTTOM, LEFT_RIGHT)
      isVisible = name.isNotEmpty()
    }

    if (isCollapsible) {
      titleLabel.addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent?) {
          if (isCollapsible) {
            titleLabel.icon = if (courseCardsPanel.isVisible) AllIcons.General.ArrowRight else AllIcons.General.ArrowDown
          }
          courseCardsPanel.isVisible = !courseCardsPanel.isVisible
        }
      })
    }

    return titleLabel
  }

  private fun fillCourseCardsPanel(coursesGroup: CoursesGroup, createCourseCard: (Course) -> CourseCardComponent) {
    for (course in coursesGroup.courses) {
      val courseCardComponent = createCourseCard(course)
      courseCardComponent.updateColors(false)
      courseCardsPanel.add(courseCardComponent)
    }
  }

  val courseCards: List<CourseCardComponent>
    get() = courseCardsPanel.components.filterIsInstance<CourseCardComponent>()
}