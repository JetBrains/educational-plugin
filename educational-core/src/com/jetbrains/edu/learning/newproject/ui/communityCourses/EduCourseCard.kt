package com.jetbrains.edu.learning.newproject.ui.communityCourses

import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.CourseCardComponent
import com.jetbrains.edu.learning.newproject.ui.CoursesDialogFontManager
import com.jetbrains.edu.learning.newproject.ui.GRAY_COLOR
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.TypographyManager
import icons.EducationalCoreIcons
import java.awt.Font
import javax.swing.JPanel

private const val H_GAP = 10
private const val INFO_HGAP = 0

class EduCourseCard(course: Course) : CourseCardComponent(course) {

  override fun createBottomComponent(): JPanel {
    return EduCourseInfoComponent(course as EduCourse)
  }
}

private class EduCourseInfoComponent(course: EduCourse) : JPanel(HorizontalLayout(INFO_HGAP)) {

  init {
    border = JBUI.Borders.emptyBottom(5)

    val componentsFont = Font(TypographyManager().bodyFont, Font.PLAIN, CoursesDialogFontManager.smallCardFontSize)

    val rating = JBLabel().apply {
      foreground = GRAY_COLOR
      border = JBUI.Borders.emptyRight(H_GAP)
      if (course.reviewScore != 0.0) {
        icon = AllIcons.Plugins.Rating
        text = "%.${1}f".format(course.reviewScore)
      }
      else {
        text = EduCoreBundle.message("course.dialog.card.not.rated")
      }
      font = componentsFont
    }

    val downloads = JBLabel().apply {
      foreground = GRAY_COLOR
      icon = EducationalCoreIcons.User
      text = course.learnersCount.toString()
      border = JBUI.Borders.emptyRight(H_GAP)
      font = componentsFont
    }

    val authorComponent = JBLabel().apply {
      val authors = course.authorFullNames.joinToString()
      isVisible = authors.isNotEmpty()
      foreground = GRAY_COLOR
      font = componentsFont
      if (this.isVisible) {
        text = authors
      }
    }

    add(rating)
    add(downloads)
    add(authorComponent)
  }
}