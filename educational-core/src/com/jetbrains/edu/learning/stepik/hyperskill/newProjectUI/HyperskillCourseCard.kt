package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.HyperskillCourseAdvertiser
import com.jetbrains.edu.learning.newproject.ui.coursesList.CourseCardComponent
import com.jetbrains.edu.learning.newproject.ui.CoursesDialogFontManager
import com.jetbrains.edu.learning.newproject.ui.GRAY_COLOR
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.TypographyManager
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.JLabel
import javax.swing.JPanel

private const val INFO_HGAP = 0
private const val INFO_VGAP = 5

class HyperskillCourseCard(course: Course) : CourseCardComponent(course) {

  override fun createBottomComponent(): JPanel {
    return AcademyCourseInfoComponent(course is HyperskillCourseAdvertiser)
  }
}

private class AcademyCourseInfoComponent(isAdvertisingCourse: Boolean) : JPanel(FlowLayout(FlowLayout.LEFT, INFO_HGAP, INFO_VGAP)) {

  init {
    val commentLabel = JLabel().apply {
      foreground = GRAY_COLOR
      font = Font(TypographyManager().bodyFont, Font.PLAIN, CoursesDialogFontManager.smallCardFontSize)
      if (isAdvertisingCourse) {
        text = EduCoreBundle.message("course.dialog.jba.default.card.info")
      }
    }

    add(commentLabel)
  }
}