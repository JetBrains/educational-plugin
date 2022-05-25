package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.openapi.util.NlsSafe
import com.jetbrains.edu.learning.newproject.ui.CoursesDialogFontManager
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.TypographyManager
import org.jetbrains.annotations.Nls
import java.awt.Font

class CourseNameHtmlPanel : CourseHtmlPanel(), CourseSelectionListener {

  @Nls
  override fun getBody(): String {
    course?.let {
      return wrapWithTags(it.name)
    }
    return ""
  }

  @Suppress("UnstableApiUsage")
  @NlsSafe
  private fun wrapWithTags(@NlsSafe courseName: String) = "<html><span><b>$courseName</b></span></html>"

  override fun getBodyFont(): Font = Font(TypographyManager().bodyFont, Font.BOLD, CoursesDialogFontManager.headerFontSize)

  override fun onCourseSelectionChanged(data: CourseBindData) {
    super.bind(data.course)
  }
}
