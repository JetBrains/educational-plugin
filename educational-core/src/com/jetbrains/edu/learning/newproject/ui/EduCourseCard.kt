package com.jetbrains.edu.learning.newproject.ui

import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.TypographyManager
import java.awt.Font
import javax.swing.JPanel

class EduCourseCard(course: Course) : CourseCardComponent(course) {

  override fun createBottomComponent(): JPanel {
    return EduCourseInfoComponent(course as EduCourse)
  }
}

private class EduCourseInfoComponent(course: EduCourse) : JPanel(HorizontalLayout(0)) {

  init {
    border = JBUI.Borders.emptyBottom(5)

    val componentsFont = Font(TypographyManager().bodyFont, Font.PLAIN, CoursesDialogFontManager.smallCardFontSize)

    val rating = JBLabel().apply {
      foreground = GRAY_COLOR
      border = JBUI.Borders.emptyRight(COURSE_CARD_BOTTOM_LABEL_H_GAP)
      if (course.reviewScore != 0.0) {
        icon = AllIcons.Plugins.Rating
        text = "%.${1}f".format(course.reviewScore)
      }
      else {
        text = EduCoreBundle.message("course.dialog.card.not.rated")
      }
      font = componentsFont
    }

    val downloads = createUsersNumberLabel(course.learnersCount)

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