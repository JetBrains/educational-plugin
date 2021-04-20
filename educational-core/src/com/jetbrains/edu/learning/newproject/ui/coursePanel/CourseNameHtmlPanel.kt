package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.jetbrains.edu.learning.newproject.ui.CoursesDialogFontManager
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.TypographyManager
import java.awt.Font

class CourseNameHtmlPanel : CourseHtmlPanel(), CourseSelectionListener {

  override fun getBody(): String {
    course?.let {
      return "<html><span>${it.name ?: ""}</span></html>"
    }
    return ""
  }

  override fun getBodyFont(): Font = Font(TypographyManager().bodyFont, Font.BOLD, CoursesDialogFontManager.headerFontSize)

  override fun onCourseSelectionChanged(courseInfo: CourseInfo, courseDisplaySettings: CourseDisplaySettings) {
    super.bind(courseInfo.course)
  }
}