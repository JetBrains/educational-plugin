package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.GrayTextHtmlPanel
import javax.swing.ScrollPaneConstants


private const val INFO_PANEL_TOP_OFFSET = 7

class AuthorsPanel : JBScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                                  ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), CourseSelectionListener {
  private var authorsLabel: GrayTextHtmlPanel = GrayTextHtmlPanel("text").apply {
    border = JBUI.Borders.empty(INFO_PANEL_TOP_OFFSET, HORIZONTAL_MARGIN, 0, 0)
  }

  private val Course.allAuthors: String
    get() = course.authorFullNames.joinToString()

  init {
    setViewportView(authorsLabel)
    border = JBUI.Borders.empty()
  }

  override fun onCourseSelectionChanged(data: CourseBindData) {
    val (course, courseDisplaySettings) = data
    isVisible = courseDisplaySettings.showInstructorField && course.allAuthors.isNotEmpty()
    if (authorsLabel.isVisible) {
      authorsLabel.setBody("by ${course.allAuthors}")
    }
  }
}
